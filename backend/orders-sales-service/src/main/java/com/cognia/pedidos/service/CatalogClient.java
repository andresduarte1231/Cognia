package com.cognia.pedidos.service;

import com.cognia.pedidos.config.JwtConfig;
import com.cognia.pedidos.dto.Dtos.ProductoCatalogo;
import com.cognia.pedidos.exception.ConflictException;
import com.cognia.pedidos.exception.ServicioNoDisponibleException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Cliente HTTP del servicio de catalogo: timeouts, token de servicio de vida corta, un reintento solo en operaciones idempotentes. */
@Component
public class CatalogClient {
    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private final RestClient http;
    private final JwtEncoder encoder;
    private final MeterRegistry registry;

    public CatalogClient(@Value("${cognia.catalog-url}") String url, @Value("${cognia.http-timeout-ms}") int timeout,
                         @Value("${cognia.jwt-secret}") String secret, MeterRegistry registry) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeout);
        f.setReadTimeout(timeout);
        this.http = RestClient.builder().baseUrl(url).requestFactory(f).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(JwtConfig.key(secret)));
        this.registry = registry;
    }

    private String tokenServicio() {
        Instant now = Instant.now();
        JwtClaimsSet c = JwtClaimsSet.builder().issuer("cognia-orders").subject("orders-service").issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(2)))
            .claim("rol", "SERVICIO").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), c)).getTokenValue();
    }

    public Map<Long, ProductoCatalogo> productos(List<Long> ids) {
        String lista = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<ProductoCatalogo> l = ejecutar("consulta_productos", true, () -> http.get().uri("/api/productos/lote?ids=" + lista)
            .header("X-Correlation-Id", corr()).retrieve().body(new ParameterizedTypeReference<List<ProductoCatalogo>>() {}));
        if (l == null) throw new ServicioNoDisponibleException("Respuesta invalida del catalogo");
        return l.stream().collect(Collectors.toMap(ProductoCatalogo::id, p -> p));
    }

    public String costoDomicilio() {
        try {
            Map<String, String> cfg = ejecutar("consulta_configuracion", true, () -> http.get().uri("/api/configuracion/publica")
                .header("X-Correlation-Id", corr()).retrieve().body(new ParameterizedTypeReference<Map<String, String>>() {}));
            return cfg == null ? "5000" : cfg.getOrDefault("costo_domicilio", "5000");
        } catch (ServicioNoDisponibleException e) {
            throw e;
        }
    }

    public void reservar(Long pedidoId, Map<Long, Integer> items) {
        var body = Map.of("pedidoId", pedidoId, "items", items.entrySet().stream().map(e -> Map.of("productoId", e.getKey(), "cantidad", e.getValue())).toList());
        post("/internal/inventario/reservas", body, "reservar");
    }

    public void liberar(Long pedidoId) { post("/internal/inventario/liberaciones", Map.of("pedidoId", pedidoId), "liberar"); }

    public void confirmar(Long pedidoId) { post("/internal/inventario/confirmaciones", Map.of("pedidoId", pedidoId), "confirmar"); }

    /** Las tres operaciones de inventario son idempotentes por pedidoId, por eso admiten un reintento. */
    private void post(String path, Object body, String op) {
        ejecutar(op, true, () -> http.post().uri(path).header("Authorization", "Bearer " + tokenServicio()).header("X-Correlation-Id", corr())
            .body(body).retrieve().toBodilessEntity());
    }

    private <T> T ejecutar(String op, boolean reintentar, java.util.function.Supplier<T> call) {
        int intentos = reintentar ? 2 : 1;
        RuntimeException ultimo = null;
        for (int i = 1; i <= intentos; i++) {
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 409) throw new ConflictException(mensaje(e));
                registry.counter("cognia_comunicacion_errores_total", "destino", "catalogo", "tipo", "http_4xx").increment();
                throw new ServicioNoDisponibleException("El catalogo rechazo la solicitud");
            } catch (RestClientResponseException e) {
                ultimo = e;
                registry.counter("cognia_comunicacion_errores_total", "destino", "catalogo", "tipo", "http_5xx").increment();
            } catch (ResourceAccessException e) {
                ultimo = e;
                registry.counter("cognia_comunicacion_errores_total", "destino", "catalogo", "tipo", "conexion").increment();
            }
            log.warn("Fallo comunicacion con catalogo op={} intento={}/{}", op, i, intentos);
        }
        throw new ServicioNoDisponibleException("El servicio de catalogo no esta disponible, intenta nuevamente");
    }

    private String mensaje(HttpClientErrorException e) {
        try {
            var n = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
            return n.path("mensaje").asText("Conflicto de inventario");
        } catch (Exception ex) {
            return "Conflicto de inventario";
        }
    }

    private String corr() {
        String c = MDC.get("correlationId");
        return c == null ? java.util.UUID.randomUUID().toString() : c;
    }
}