package com.cognia.auth.controller;

import com.cognia.auth.dto.Dtos.*;
import com.cognia.auth.entity.Usuario;
import com.cognia.auth.service.UsuarioAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminUsuarioController {
    private final UsuarioAdminService svc;

    public AdminUsuarioController(UsuarioAdminService svc) { this.svc = svc; }

    @GetMapping("/usuarios")
    public PageDto<UsuarioDto> listar(@RequestParam(required = false) Usuario.Rol rol, @RequestParam(required = false) Usuario.Estado estado,
                                      @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return svc.listar(rol, estado, q, pagina, tamano);
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioDto obtener(@PathVariable Long id) { return svc.obtener(id); }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioAdminRequest r) { return svc.crear(r); }

    @PutMapping("/usuarios/{id}")
    public UsuarioDto editar(@PathVariable Long id, @Valid @RequestBody EditarUsuarioRequest r) { return svc.editar(id, r); }

    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioDto estado(@PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest r) { return svc.cambiarEstado(id, r.estado()); }

    @PostMapping("/usuarios/{id}/restablecer-password")
    public PasswordTemporalResponse restablecer(@PathVariable Long id) { return svc.restablecerPassword(id); }

    @GetMapping("/auditoria")
    public PageDto<AuditoriaDto> auditoria(@RequestParam(required = false) String accion, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return svc.auditoria(accion, pagina, tamano);
    }
}