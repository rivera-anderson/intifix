package com.intifix.chatnotif.chat.gateway.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.intifix.chatnotif.chat.gateway.ChatGateway;
import com.intifix.chatnotif.shared.client.MonolithClient;

import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto {@link ChatGateway} sobre HTTP. En el monolito este
 * puerto leía Postgres con JDBC; aquí pregunta a la API interna del monolito.
 * El resto del módulo chat no cambió.
 */
@Component
@RequiredArgsConstructor
public class HttpChatGatewayAdapter implements ChatGateway {

    private final MonolithClient monolithClient;

    @Override
    public boolean existeUsuario(UUID idUsuario) {
        if (idUsuario == null) {
            return false;
        }
        return monolithClient.estadoUsuario(idUsuario)
                .map(MonolithClient.EstadoUsuario::existe)
                .orElse(false);
    }

    @Override
    public Optional<ServicioParticipantes> obtenerParticipantes(UUID idServicio) {
        if (idServicio == null) {
            return Optional.empty();
        }
        return monolithClient.participantes(idServicio)
                .map(p -> new ServicioParticipantes(p.idServicio(), p.idCliente(), p.idTecnico()));
    }
}
