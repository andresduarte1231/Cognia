package com.cognia.auth.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Limita intentos fallidos por clave (ip|correo) en memoria. Limitacion: se reinicia con el proceso y no se comparte entre instancias. */
@Component
public class LoginAttemptLimiter {
    static final int MAX = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public boolean bloqueado(String key) {
        Deque<Instant> d = fallos.get(key);
        if (d == null) return false;
        synchronized (d) {
            purge(d);
            return d.size() >= MAX;
        }
    }

    public void fallo(String key) {
        Deque<Instant> d = fallos.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) { purge(d); d.addLast(Instant.now()); }
    }

    public void exito(String key) { fallos.remove(key); }

    private void purge(Deque<Instant> d) {
        Instant limit = Instant.now().minus(WINDOW);
        while (!d.isEmpty() && d.peekFirst().isBefore(limit)) d.pollFirst();
    }
}