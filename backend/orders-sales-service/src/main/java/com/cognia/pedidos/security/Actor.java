package com.cognia.pedidos.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Actor {
    private Actor() {}

    public static Long id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        try { return Long.valueOf(a.getName()); } catch (NumberFormatException e) { return null; }
    }

    public static Set<String> roles() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return Set.of();
        return a.getAuthorities().stream().map(g -> g.getAuthority().replace("ROLE_", "")).collect(Collectors.toSet());
    }

    public static boolean esPersonal() {
        Set<String> r = roles();
        return r.contains("PROPIETARIO") || r.contains("ADMINISTRADOR") || r.contains("EMPLEADO_VENTAS");
    }
}