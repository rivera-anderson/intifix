package com.intifix.chatnotif.shared.messaging;

import java.security.Principal;
import java.util.UUID;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.intifix.chatnotif.shared.events.ChatMessageSentEvent;
import com.intifix.chatnotif.shared.events.ChatSessionEvent;
import com.intifix.chatnotif.shared.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica en RabbitMQ la actividad del chat para que la auditoría del
 * monolito la registre (antes era un evento interno de Spring entre módulos).
 * Si RabbitMQ no está disponible se registra el error y el chat sigue
 * funcionando: la auditoría no debe tumbar el envío de mensajes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void alEnviarMensaje(ChatMessageSentEvent evento) {
        publicar(Eventos.CHAT_MENSAJE_ENVIADO, evento);
    }

    @EventListener
    public void alConectar(SessionConnectedEvent evento) {
        UUID userId = userId(evento.getUser());
        if (userId != null) {
            publicar(Eventos.CHAT_SESION_CONECTADA, new ChatSessionEvent(userId));
        }
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent evento) {
        UUID userId = userId(evento.getUser());
        if (userId != null) {
            publicar(Eventos.CHAT_SESION_DESCONECTADA, new ChatSessionEvent(userId));
        }
    }

    private void publicar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(Eventos.EXCHANGE, routingKey, evento);
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} en RabbitMQ: {}", routingKey, e.getMessage());
        }
    }

    private static UUID userId(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof AuthenticatedUser usuario) {
            return usuario.getId();
        }
        // El principal de la sesión STOMP es el token de autenticación; su nombre es el UUID.
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
