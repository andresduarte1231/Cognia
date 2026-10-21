package com.cognia.gateway;

import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Sirve index.html para las rutas del frontend React (React Router) al recargar o abrir un enlace directo. */
@Component
public class SpaFallbackFilter implements WebFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        boolean esApi = path.startsWith("/api/") || path.startsWith("/actuator") || path.startsWith("/uploads/");
        String ultimo = path.substring(path.lastIndexOf('/') + 1);
        boolean esArchivo = ultimo.contains(".");
        if (HttpMethod.GET.equals(exchange.getRequest().getMethod()) && !esApi && !esArchivo && !path.equals("/index.html")) {
            return chain.filter(exchange.mutate().request(exchange.getRequest().mutate().path("/index.html").build()).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 10; }
}