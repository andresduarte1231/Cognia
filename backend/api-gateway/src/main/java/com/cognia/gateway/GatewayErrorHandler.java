package com.cognia.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Errores del gateway en JSON uniforme, sin detalles internos. Cuenta errores de comunicacion con servicios. */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private final ObjectMapper mapper;
    private final MeterRegistry registry;

    public GatewayErrorHandler(ObjectMapper mapper, MeterRegistry registry) { this.mapper = mapper; this.registry = registry; }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "ERROR_INTERNO", msg = "Ocurrio un error interno";
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof ConnectException || root.getClass().getSimpleName().contains("Connect")) {
            status = HttpStatus.SERVICE_UNAVAILABLE; code = "SERVICIO_NO_DISPONIBLE"; msg = "El servicio solicitado no esta disponible";
            registry.counter("cognia_comunicacion_errores_total", "destino", "gateway", "tipo", "conexion").increment();
        } else if (root instanceof TimeoutException || root.getClass().getSimpleName().contains("Timeout")) {
            status = HttpStatus.GATEWAY_TIMEOUT; code = "TIEMPO_AGOTADO"; msg = "El servicio tardo demasiado en responder";
            registry.counter("cognia_comunicacion_errores_total", "destino", "gateway", "tipo", "timeout").increment();
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = status == HttpStatus.NOT_FOUND ? "NO_ENCONTRADO" : "ERROR_SOLICITUD";
            msg = status == HttpStatus.NOT_FOUND ? "Recurso no encontrado" : "No fue posible procesar la solicitud";
        } else {
            log.error("Error no controlado en gateway", ex);
        }
        if (exchange.getResponse().isCommitted()) return Mono.error(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] b = mapper.writeValueAsBytes(Map.of("codigo", code, "mensaje", msg, "estado", status.value()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(b)));
        } catch (Exception e) {
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap("{}".getBytes(StandardCharsets.UTF_8))));
        }
    }
}