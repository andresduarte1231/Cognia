package com.cognia.auth.security;

import com.cognia.auth.config.JwtConfig;
import com.cognia.auth.entity.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final JwtEncoder encoder;
    private final Duration ttl;

    public JwtService(@Value("${cognia.jwt-secret}") String secret, @Value("${cognia.jwt-ttl-minutes}") long minutes) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(JwtConfig.key(secret)));
        this.ttl = Duration.ofMinutes(minutes);
    }

    public long ttlSeconds() { return ttl.toSeconds(); }

    public String emitir(Usuario u) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("cognia-auth").subject(String.valueOf(u.getId()))
            .issuedAt(now).expiresAt(now.plus(ttl)).id(UUID.randomUUID().toString()).claim("rol", u.getRol().name()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}