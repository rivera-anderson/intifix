package com.intifix.chatnotif.shared.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.intifix.chatnotif.shared.client.MonolithClient;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Autentica las requests HTTP con el JWT del monolito y, como hacía el
 * monolito, verifica que la cuenta siga activa (baneos y suspensiones).
 * El estado se consulta a la API interna del monolito con caché corta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final MonolithClient monolithClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        var usuario = jwtService.autenticar(header.substring(BEARER_PREFIX.length())).orElse(null);
        if (usuario == null) {
            chain.doFilter(request, response);
            return;
        }

        // Falla cerrado: si no se puede confirmar que la cuenta está activa, no se autentica.
        boolean activo = monolithClient.estadoUsuario(usuario.getId())
                .map(e -> e.existe() && e.activo())
                .orElse(false);
        if (!activo) {
            log.warn("Acceso denegado al usuario {}: cuenta inactiva o estado no verificable", usuario.getId());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "La cuenta no se encuentra activa.");
            return;
        }

        var authorities = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null, authorities));
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/ws/")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }
}
