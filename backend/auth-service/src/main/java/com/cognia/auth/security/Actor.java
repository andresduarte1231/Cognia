package com.cognia.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Actor {
    private Actor() {}

    public static Long id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        try { return Long.valueOf(a.getName()); } catch (NumberFormatException e) { return null; }
    }
}