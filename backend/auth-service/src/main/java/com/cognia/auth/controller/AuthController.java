package com.cognia.auth.controller;

import com.cognia.auth.dto.Dtos.*;
import com.cognia.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto registro(@Valid @RequestBody RegistroRequest r) { return auth.registrar(r); }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) { return auth.login(r, req.getRemoteAddr()); }

    @PostMapping("/renovar")
    public TokenResponse renovar() { return auth.renovar(); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() { auth.logout(); }

    @GetMapping("/me")
    public UsuarioDto me() { return auth.perfil(); }

    @PutMapping("/me")
    public UsuarioDto actualizar(@Valid @RequestBody PerfilRequest r) { return auth.actualizarPerfil(r); }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody CambioPasswordRequest r) { auth.cambiarPassword(r); }

    @PostMapping("/inicializar-propietario")
    public UsuarioDto inicializar(@Valid @RequestBody InicializarPropietarioRequest r, @RequestHeader(value = "X-Bootstrap-Token", required = false) String token) {
        return auth.inicializarPropietario(r, token);
    }
}