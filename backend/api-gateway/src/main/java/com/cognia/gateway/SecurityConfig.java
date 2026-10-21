package com.cognia.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy;

/**
 * El gateway no autoriza el trafico de negocio (cada microservicio valida el JWT); solo protege sus propios
 * endpoints de operacion (metricas) y aplica cabeceras de seguridad. /internal/** y /actuator/** de los servicios no tienen ruta.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    MapReactiveUserDetailsService users(@Value("${cognia.metrics-user}") String u, @Value("${cognia.metrics-password}") String p) {
        if (p == null || p.isBlank()) {
            return new MapReactiveUserDetailsService(User.withUsername("deshabilitado").password("{noop}x").roles("NONE").disabled(true).build());
        }
        return new MapReactiveUserDetailsService(User.withUsername(u).password(new BCryptPasswordEncoder().encode(p)).roles("METRICS").build());
    }

    @Bean
    SecurityWebFilterChain chain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(Customizer.withDefaults())
            .headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; img-src 'self' data: https:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .authorizeExchange(a -> a
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .pathMatchers("/actuator/**").hasRole("METRICS")
                .anyExchange().permitAll())
            .build();
    }
}