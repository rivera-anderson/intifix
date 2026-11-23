package com.intifix.chatnotif.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Verifica que este servicio acepta exactamente los tokens que emite el monolito. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-de-al-menos-32-bytes-para-hs256!!";
    private final JwtService jwtService = new JwtService(SECRET);

    private static String token(String secret, String typ, Instant expira, UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("correo", "cliente@intifix.com")
                .claim("roles", List.of("CLIENTE"))
                .claim("typ", typ)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expira))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void aceptaAccessTokenValido() {
        UUID id = UUID.randomUUID();
        var usuario = jwtService.autenticar(token(SECRET, "access", Instant.now().plusSeconds(60), id)).orElseThrow();
        assertEquals(id, usuario.getId());
        assertTrue(usuario.tieneRol("CLIENTE"));
    }

    @Test
    void rechazaRefreshToken() {
        String refresh = token(SECRET, "refresh", Instant.now().plusSeconds(60), UUID.randomUUID());
        assertTrue(jwtService.autenticar(refresh).isEmpty());
    }

    @Test
    void rechazaTokenExpirado() {
        String expirado = token(SECRET, "access", Instant.now().minusSeconds(60), UUID.randomUUID());
        assertTrue(jwtService.autenticar(expirado).isEmpty());
    }

    @Test
    void rechazaFirmaDeOtroSecreto() {
        String ajeno = token("otro-secreto-distinto-de-32-bytes-o-mas!!!!", "access",
                Instant.now().plusSeconds(60), UUID.randomUUID());
        assertTrue(jwtService.autenticar(ajeno).isEmpty());
    }

    @Test
    void noArrancaConSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto"));
    }
}
