package com.intifix.ai.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Valida los access tokens que emite el monolito (HS256 con JWT_SECRET).
 * Este servicio nunca emite tokens: solo los verifica.
 *
 * <p>Contrato del token (definido en el monolito): {@code sub} = UUID del
 * usuario, {@code correo}, {@code roles} y {@code typ} = "access".</p>
 */
@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;

    public JwtService(@Value("${intifix.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "intifix.jwt.secret debe tener al menos 32 bytes. Configure JWT_SECRET (el mismo del monolito).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Devuelve el usuario si el token es un access token válido; vacío si es
     * inválido, expiró o es un refresh token.
     */
    public Optional<AuthenticatedUser> autenticar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                return Optional.empty();
            }
            return Optional.of(AuthenticatedUser.builder()
                    .id(UUID.fromString(claims.getSubject()))
                    .correo(claims.get("correo", String.class))
                    .roles(Set.copyOf(roles(claims)))
                    .build());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> roles(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return roles != null ? roles : List.of();
    }
}
