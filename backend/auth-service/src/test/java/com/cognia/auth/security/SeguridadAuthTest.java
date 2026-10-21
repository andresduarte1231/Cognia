package com.cognia.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognia.auth.config.JwtConfig;
import com.cognia.auth.entity.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class SeguridadAuthTest {
    static final String SECRET = "una-clave-de-prueba-muy-larga-de-al-menos-32-bytes!!";

    private static NimbusJwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(JwtConfig.key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static Usuario usuario() {
        Usuario u = new Usuario(); u.setId(7L); u.setRol(Usuario.Rol.CLIENTE);
        return u;
    }

    @Test
    void tokenEmitidoSeValidaYContieneRolYSujeto() {
        String token = new JwtService(SECRET, 60).emitir(usuario());
        Jwt jwt = decoder(SECRET).decode(token);
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.<String>getClaim("rol")).isEqualTo("CLIENTE");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        String token = new JwtService("otra-clave-distinta-de-al-menos-32-bytes-xxxx", 60).emitir(usuario());
        assertThatThrownBy(() -> decoder(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenManipuladoEsRechazado() {
        String token = new JwtService(SECRET, 60).emitir(usuario());
        String[] partes = token.split("\\.");
        String falso = partes[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\",\"rol\":\"PROPIETARIO\"}".getBytes()) + "." + partes[2];
        assertThatThrownBy(() -> decoder(SECRET).decode(falso)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSinFirmaAlgNoneEsRechazado() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String body = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\",\"rol\":\"PROPIETARIO\"}".getBytes());
        assertThatThrownBy(() -> decoder(SECRET).decode(header + "." + body + ".")).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoEsRechazado() {
        JwtEncoder enc = new NimbusJwtEncoder(new ImmutableSecret<>(JwtConfig.key(SECRET)));
        Instant antes = Instant.now().minusSeconds(3600);
        JwtClaimsSet c = JwtClaimsSet.builder().subject("7").issuedAt(antes).expiresAt(antes.plusSeconds(60)).claim("rol", "CLIENTE").build();
        String token = enc.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), c)).getTokenValue();
        assertThatThrownBy(() -> decoder(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void limitadorBloqueaTrasCincoFallosYSeReiniciaConExito() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) l.fallo("ip|a@b.c");
        assertThat(l.bloqueado("ip|a@b.c")).isFalse();
        l.fallo("ip|a@b.c");
        assertThat(l.bloqueado("ip|a@b.c")).isTrue();
        assertThat(l.bloqueado("ip|otro@b.c")).isFalse();
        l.exito("ip|a@b.c");
        assertThat(l.bloqueado("ip|a@b.c")).isFalse();
    }

    @Test
    void bcryptNoAlmacenaTextoPlano() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("Clave1234");
        assertThat(hash).doesNotContain("Clave1234").startsWith("$2");
        assertThat(enc.matches("Clave1234", hash)).isTrue();
        assertThat(enc.matches("clave1234", hash)).isFalse();
    }
}