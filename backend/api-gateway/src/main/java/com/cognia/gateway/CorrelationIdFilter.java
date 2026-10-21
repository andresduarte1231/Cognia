package com.cognia.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Genera o valida X-Correlation-Id, lo propaga a los servicios y lo devuelve al cliente. */
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (id == null || !id.matches("[A-Za-z0-9-]{8,64}")) id = UUID.randomUUID().toString();
        final String cid = id;
        ServerHttpRequest req = exchange.getRequest().mutate().header(HEADER, cid).build();
        exchange.getResponse().beforeCommit(() -> { exchange.getResponse().getHeaders().set(HEADER, cid); return Mono.empty(); });
        return chain.filter(exchange.mutate().request(req).build());
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}