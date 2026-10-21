package com.cognia.auth.service;

import com.cognia.auth.dto.Dtos.*;
import com.cognia.auth.entity.Usuario;
import com.cognia.auth.entity.Usuario.Estado;
import com.cognia.auth.entity.Usuario.Rol;
import com.cognia.auth.exception.BusinessException;
import com.cognia.auth.exception.ConflictException;
import com.cognia.auth.exception.NotFoundException;
import com.cognia.auth.mapper.UsuarioMapper;
import com.cognia.auth.repository.AuditoriaRepository;
import com.cognia.auth.repository.UsuarioRepository;
import com.cognia.auth.security.Actor;
import java.security.SecureRandom;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion de cuentas. Regla: el rol PROPIETARIO no se asigna ni se modifica por esta API (solo por el procedimiento de
 * inicializacion); nadie puede cambiar su propio rol/estado. Los controles se repiten aqui aunque SecurityConfig ya restrinja la ruta.
 */
@Service
public class UsuarioAdminService {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final UsuarioRepository usuarios;
    private final AuditoriaRepository auditorias;
    private final PasswordEncoder encoder;
    private final UsuarioMapper mapper;
    private final AuditoriaService auditoria;
    private final SecureRandom random = new SecureRandom();

    public UsuarioAdminService(UsuarioRepository usuarios, AuditoriaRepository auditorias, PasswordEncoder encoder, UsuarioMapper mapper, AuditoriaService auditoria) {
        this.usuarios = usuarios; this.auditorias = auditorias; this.encoder = encoder; this.mapper = mapper; this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageDto<UsuarioDto> listar(Rol rol, Estado estado, String q, int page, int size) {
        var p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        return mapper.page(usuarios.buscar(rol, estado, q == null || q.isBlank() ? null : q.trim(), p), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) { return mapper.toDto(buscar(id)); }

    @Transactional
    public UsuarioDto crear(UsuarioAdminRequest r) {
        exigirPropietario();
        if (r.rol() == Rol.PROPIETARIO) throw new AccessDeniedException("El rol PROPIETARIO no puede asignarse");
        if (r.password() == null) throw new BusinessException("La contrasena es obligatoria");
        String correo = r.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ConflictException("El correo ya esta registrado");
        Usuario u = new Usuario();
        u.setNombre(r.nombre().trim()); u.setCorreo(correo); u.setRol(r.rol()); u.setPasswordHash(encoder.encode(r.password()));
        u = usuarios.save(u);
        auditoria.ok("CREAR_USUARIO", "usuarios", u.getId(), "rol=" + u.getRol());
        return mapper.toDto(u);
    }

    @Transactional
    public UsuarioDto editar(Long id, EditarUsuarioRequest r) {
        exigirPropietario();
        Usuario u = buscar(id);
        protegerObjetivo(u);
        if (r.rol() == Rol.PROPIETARIO) throw new AccessDeniedException("El rol PROPIETARIO no puede asignarse");
        u.setNombre(r.nombre().trim());
        u.setRol(r.rol());
        auditoria.ok("EDITAR_USUARIO", "usuarios", id, "rol=" + r.rol());
        return mapper.toDto(usuarios.save(u));
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, Estado estado) {
        exigirPropietario();
        Usuario u = buscar(id);
        protegerObjetivo(u);
        u.setEstado(estado);
        auditoria.ok("CAMBIAR_ESTADO_USUARIO", "usuarios", id, estado.name());
        return mapper.toDto(usuarios.save(u));
    }

    @Transactional
    public PasswordTemporalResponse restablecerPassword(Long id) {
        exigirPropietario();
        Usuario u = buscar(id);
        protegerObjetivo(u);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        String temp = sb + "a1";
        u.setPasswordHash(encoder.encode(temp));
        usuarios.save(u);
        auditoria.ok("RESTABLECER_PASSWORD_USUARIO", "usuarios", id, null);
        return new PasswordTemporalResponse(temp);
    }

    @Transactional(readOnly = true)
    public PageDto<AuditoriaDto> auditoria(String accion, int page, int size) {
        var p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return mapper.page(auditorias.listar(accion == null || accion.isBlank() ? null : accion, p), mapper::toDto);
    }

    private void exigirPropietario() {
        var a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getAuthorities().stream().noneMatch(g -> g.getAuthority().equals("ROLE_PROPIETARIO"))) {
            throw new AccessDeniedException("Solo el propietario administra cuentas");
        }
    }

    private void protegerObjetivo(Usuario u) {
        if (u.getId().equals(Actor.id())) throw new AccessDeniedException("No puedes modificar tu propia cuenta por esta via");
        if (u.getRol() == Rol.PROPIETARIO) throw new AccessDeniedException("La cuenta de propietario no puede modificarse");
    }

    private Usuario buscar(Long id) { return usuarios.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado")); }
}