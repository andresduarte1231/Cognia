package com.cognia.auth.service;

import com.cognia.auth.dto.Dtos.*;
import com.cognia.auth.entity.Usuario;
import com.cognia.auth.exception.BusinessException;
import com.cognia.auth.exception.ConflictException;
import com.cognia.auth.exception.NotFoundException;
import com.cognia.auth.mapper.UsuarioMapper;
import com.cognia.auth.repository.UsuarioRepository;
import com.cognia.auth.security.Actor;
import com.cognia.auth.security.JwtService;
import com.cognia.auth.security.LoginAttemptLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UsuarioMapper mapper;
    private final AuditoriaService auditoria;
    private final LoginAttemptLimiter limiter;
    private final String bootstrapToken;
    private final String dummyHash;
    private final Counter loginOk;
    private final Counter failCred;
    private final Counter failLocked;
    private final Counter registros;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, UsuarioMapper mapper, AuditoriaService auditoria,
                       LoginAttemptLimiter limiter, MeterRegistry reg, @Value("${cognia.owner-bootstrap-token}") String bootstrapToken) {
        this.usuarios = usuarios; this.encoder = encoder; this.jwt = jwt; this.mapper = mapper; this.auditoria = auditoria; this.limiter = limiter;
        this.bootstrapToken = bootstrapToken;
        this.dummyHash = encoder.encode("no-existe-" + System.nanoTime());
        this.loginOk = Counter.builder("cognia_auth_logins_total").tag("resultado", "ok").register(reg);
        this.failCred = Counter.builder("cognia_auth_failures_total").tag("motivo", "credenciales").register(reg);
        this.failLocked = Counter.builder("cognia_auth_failures_total").tag("motivo", "limite_intentos").register(reg);
        this.registros = Counter.builder("cognia_auth_registros_total").register(reg);
    }

    @Transactional
    public UsuarioDto registrar(RegistroRequest r) {
        String correo = r.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ConflictException("No fue posible completar el registro con esos datos");
        Usuario u = new Usuario();
        u.setNombre(r.nombre().trim());
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRol(Usuario.Rol.CLIENTE);
        u.setEstado(Usuario.Estado.ACTIVO);
        u = usuarios.save(u);
        registros.increment();
        auditoria.registrar(u.getId(), "REGISTRO_USUARIO", "usuarios", u.getId(), "EXITOSO", null);
        return mapper.toDto(u);
    }

    public TokenResponse login(LoginRequest r, String ip) {
        String correo = r.correo().trim().toLowerCase();
        String key = ip + "|" + correo;
        if (limiter.bloqueado(key)) {
            failLocked.increment();
            auditoria.registrar(null, "LOGIN_BLOQUEADO_LIMITE", "usuarios", null, "FALLIDO", "Demasiados intentos");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Intenta mas tarde.");
        }
        Usuario u = usuarios.findByCorreoIgnoreCase(correo).orElse(null);
        boolean ok = encoder.matches(r.password(), u != null ? u.getPasswordHash() : dummyHash) && u != null && u.getEstado() == Usuario.Estado.ACTIVO;
        if (!ok) {
            limiter.fallo(key);
            failCred.increment();
            auditoria.registrar(u != null ? u.getId() : null, "LOGIN_FALLIDO", "usuarios", u != null ? u.getId() : null, "FALLIDO", null);
            throw new CredencialesInvalidasException();
        }
        limiter.exito(key);
        loginOk.increment();
        auditoria.registrar(u.getId(), "LOGIN", "usuarios", u.getId(), "EXITOSO", null);
        return respuesta(u);
    }

    @Transactional(readOnly = true)
    public TokenResponse renovar() {
        Usuario u = actual();
        if (u.getEstado() != Usuario.Estado.ACTIVO) throw new CredencialesInvalidasException();
        return respuesta(u);
    }

    public void logout() { auditoria.ok("LOGOUT", "usuarios", Actor.id(), null); }

    @Transactional(readOnly = true)
    public UsuarioDto perfil() { return mapper.toDto(actual()); }

    @Transactional
    public UsuarioDto actualizarPerfil(PerfilRequest r) {
        Usuario u = actual();
        u.setNombre(r.nombre().trim());
        auditoria.ok("EDITAR_PERFIL", "usuarios", u.getId(), null);
        return mapper.toDto(usuarios.save(u));
    }

    @Transactional
    public void cambiarPassword(CambioPasswordRequest r) {
        Usuario u = actual();
        if (!encoder.matches(r.actual(), u.getPasswordHash())) throw new BusinessException("La contrasena actual no es correcta");
        u.setPasswordHash(encoder.encode(r.nueva()));
        usuarios.save(u);
        auditoria.ok("CAMBIO_PASSWORD", "usuarios", u.getId(), null);
    }

    /** Primer propietario (o recuperacion del propietario existente). Requiere el token de arranque definido en el entorno del servidor. */
    @Transactional
    public UsuarioDto inicializarPropietario(InicializarPropietarioRequest r, String token) {
        if (bootstrapToken == null || bootstrapToken.length() < 16 || token == null
            || !java.security.MessageDigest.isEqual(bootstrapToken.getBytes(), token.getBytes())) {
            auditoria.registrarAparte("INICIALIZAR_PROPIETARIO_RECHAZADO", "usuarios", "FALLIDO", null);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
        String correo = r.correo().trim().toLowerCase();
        Usuario existente = usuarios.findByCorreoIgnoreCase(correo).orElse(null);
        boolean hayPropietario = usuarios.existsByRolAndEstado(Usuario.Rol.PROPIETARIO, Usuario.Estado.ACTIVO);
        if (hayPropietario && (existente == null || existente.getRol() != Usuario.Rol.PROPIETARIO)) {
            throw new ConflictException("Ya existe un propietario activo; solo puede restablecerse su propia contrasena");
        }
        Usuario u = existente != null ? existente : new Usuario();
        u.setNombre(r.nombre().trim());
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRol(Usuario.Rol.PROPIETARIO);
        u.setEstado(Usuario.Estado.ACTIVO);
        u = usuarios.save(u);
        auditoria.registrar(u.getId(), existente != null ? "RESTABLECER_PROPIETARIO" : "INICIALIZAR_PROPIETARIO", "usuarios", u.getId(), "EXITOSO", null);
        return mapper.toDto(u);
    }

    private TokenResponse respuesta(Usuario u) {
        return new TokenResponse(jwt.emitir(u), "Bearer", jwt.ttlSeconds(), mapper.toDto(u));
    }

    private Usuario actual() {
        Long id = Actor.id();
        return usuarios.findById(id == null ? -1L : id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    public static class CredencialesInvalidasException extends RuntimeException {
        public CredencialesInvalidasException() { super("Credenciales invalidas"); }
    }
}