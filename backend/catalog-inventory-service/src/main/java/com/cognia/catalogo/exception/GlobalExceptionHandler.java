package com.cognia.catalogo.exception;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MeterRegistry registry;

    public GlobalExceptionHandler(MeterRegistry registry) { this.registry = registry; }

    public record ApiError(String codigo, String mensaje, int estado, List<String> detalles) {}

    private ResponseEntity<ApiError> build(HttpStatus s, String code, String msg, List<String> det) {
        return ResponseEntity.status(s).body(new ApiError(code, msg, s.value(), det));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) { return build(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of()); }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e) { return build(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), List.of()); }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException e) { return build(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_DE_NEGOCIO", e.getMessage(), List.of()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        List<String> d = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos de entrada invalidos", d);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> badRequest(Exception e) { return build(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "La solicitud no es valida", List.of()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        log.warn("Violacion de integridad: {}", e.getClass().getSimpleName());
        return build(HttpStatus.CONFLICT, "INTEGRIDAD", "La operacion viola una restriccion de datos", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException e) {
        registry.counter("cognia_security_denied_total").increment();
        return build(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tienes permisos para esta operacion", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(Exception e) { return build(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", "Metodo no permitido", List.of()); }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(Exception e) { return build(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado", List.of()); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e) {
        log.error("Error no controlado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrio un error interno", List.of());
    }
}
