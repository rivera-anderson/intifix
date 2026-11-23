package com.intifix.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class RateLimitConfigTest {

    @Test
    void usaElPrimerValorDeXForwardedFor() {
        var request = MockServerHttpRequest.get("/api/v1/services")
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.2")
                .build();
        assertEquals("203.0.113.7", RateLimitConfig.clientIp(request));
    }

    @Test
    void sinCabeceraUsaLaIpDeLaConexion() {
        var request = MockServerHttpRequest.get("/api/v1/services")
                .remoteAddress(new InetSocketAddress("198.51.100.4", 50000))
                .build();
        assertEquals("198.51.100.4", RateLimitConfig.clientIp(request));
    }
}
