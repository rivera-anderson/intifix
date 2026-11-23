package com.intifix.chatnotif.shared.security;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.intifix.chatnotif.shared.client.MonolithClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Autenticación JWT del CONNECT STOMP (igual que en el monolito), más la
 * verificación de cuenta activa.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketAuthConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final MonolithClient monolithClient;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
                    return message;
                }
                String token = extraerToken(accessor);
                if (token == null || token.isBlank()) {
                    return message;
                }
                jwtService.autenticar(token)
                        .filter(u -> monolithClient.estadoUsuario(u.getId())
                                .map(e -> e.existe() && e.activo()).orElse(false))
                        .ifPresentOrElse(usuario -> {
                            var authorities = usuario.getRoles().stream()
                                    .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                                    .toList();
                            accessor.setUser(new UsernamePasswordAuthenticationToken(usuario, null, authorities));
                        }, () -> log.debug("CONNECT STOMP rechazado: token inválido o cuenta inactiva"));
                return message;
            }
        });
    }

    private static String extraerToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        return (tokenHeaders != null && !tokenHeaders.isEmpty()) ? tokenHeaders.get(0) : null;
    }
}
