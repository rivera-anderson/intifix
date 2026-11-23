package com.intifix.gateway;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

import reactor.core.publisher.Mono;

/**
 * Clave del rate limiter: la IP real del cliente.
 *
 * En producción el gateway está detrás de Caddy, que reemplaza X-Forwarded-For
 * con la IP del cliente (no confía en el valor que manda el navegador), así que
 * el primer valor de esa cabecera es fiable. Sin proxy delante se usa la IP de
 * la conexión.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange.getRequest()));
    }

    static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(address -> address.getHostAddress())
                .orElse("unknown");
    }
}
