package com.intifix.ai.service;

import java.util.UUID;

import com.intifix.ai.dto.ChatRequest;
import com.intifix.ai.dto.PresupuestoSugeridoRequest;
import com.intifix.ai.dto.PresupuestoSugeridoResponse;

import reactor.core.publisher.Flux;

public interface AiAssistantService {

    /** Respuesta token a token. {@code idUsuario} sale siempre del JWT. */
    Flux<String> chatStream(UUID idUsuario, ChatRequest request);

    String chat(UUID idUsuario, ChatRequest request);

    void limpiarHistorial(UUID idUsuario, String conversationId);

    PresupuestoSugeridoResponse estimarPresupuesto(PresupuestoSugeridoRequest request);
}
