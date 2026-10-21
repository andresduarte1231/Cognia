package com.cognia.pedidos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Respuestas 401/403 uniformes y contadores agregados (sin etiquetas con datos personales). */
@Component
public class SecurityEventHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final Counter unauthorized;
    private final Counter denied;
    private final ObjectMapper mapper;

    public SecurityEventHandlers(MeterRegistry registry, ObjectMapper mapper) {
        this.unauthorized = Counter.builder("cognia_security_unauthorized_total").description("Solicitudes sin credenciales validas").register(registry);
        this.denied = Counter.builder("cognia_security_denied_total").description("Autorizacion denegada").register(registry);
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        unauthorized.increment();
        write(res, 401, "NO_AUTENTICADO", "Se requiere autenticacion valida");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        denied.increment();
        write(res, 403, "ACCESO_DENEGADO", "No tienes permisos para esta operacion");
    }

    private void write(HttpServletResponse res, int status, String code, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), Map.of("codigo", code, "mensaje", msg, "estado", status));
    }
}
