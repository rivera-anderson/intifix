package com.intifix.ai.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import com.intifix.ai.dto.ChatRequest;
import com.intifix.ai.dto.PresupuestoSugeridoRequest;
import com.intifix.ai.dto.PresupuestoSugeridoResponse;
import com.intifix.ai.memory.ConversationMemoryService;
import com.intifix.ai.prompt.SystemPrompts;
import com.intifix.ai.service.AiAssistantService;
import com.intifix.ai.shared.client.MonolithClient;
import com.intifix.ai.tools.ServiceTools;
import com.intifix.ai.tools.TechnicianTools;
import com.intifix.ai.tools.UserTools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Motor de IA.
 *
 * <p>Pipeline por petición: cargar memoria (MongoDB) → prompt con historial +
 * herramientas (que consultan al monolito por la API interna) → GPT-4o vía
 * {@link ChatClient} → stream → persistir el turno.</p>
 *
 * <p>La conversación se guarda con la clave {@code <idUsuario>:<conversationId>}:
 * cada usuario solo ve y borra sus propios hilos.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final ChatClient intifixChatClient;
    private final ConversationMemoryService memoryService;
    private final TechnicianTools technicianTools;
    private final ServiceTools serviceTools;
    private final MonolithClient monolithClient;

    @Override
    public Flux<String> chatStream(UUID idUsuario, ChatRequest request) {
        String hilo = hilo(idUsuario, request.getConversationId());
        List<Message> historial = memoryService.cargarHistorial(hilo);
        memoryService.guardarMensajeUsuario(hilo, request.getMessage());

        StringBuilder acumulado = new StringBuilder();
        return prompt(idUsuario, historial, request.getMessage())
                .stream()
                .content()
                .doOnNext(acumulado::append)
                .doOnComplete(() -> memoryService.guardarMensajeAsistente(hilo, acumulado.toString()))
                .doOnError(e -> log.error("Error generando respuesta de IA (stream): {}", e.getMessage(), e));
    }

    @Override
    public String chat(UUID idUsuario, ChatRequest request) {
        String hilo = hilo(idUsuario, request.getConversationId());
        List<Message> historial = memoryService.cargarHistorial(hilo);
        memoryService.guardarMensajeUsuario(hilo, request.getMessage());

        String respuesta = prompt(idUsuario, historial, request.getMessage()).call().content();
        memoryService.guardarMensajeAsistente(hilo, respuesta);
        return respuesta;
    }

    @Override
    public void limpiarHistorial(UUID idUsuario, String conversationId) {
        memoryService.limpiarHistorial(hilo(idUsuario, conversationId));
    }

    @Override
    public PresupuestoSugeridoResponse estimarPresupuesto(PresupuestoSugeridoRequest request) {
        String userContent = "Título: %s\nDescripción: %s%s".formatted(
                request.titulo(),
                request.descripcion(),
                request.especialidad() != null && !request.especialidad().isBlank()
                        ? "\nEspecialidad: " + request.especialidad()
                        : "");

        return intifixChatClient.prompt()
                .system(SystemPrompts.PRESUPUESTO_ESTIMACION)
                .user(userContent)
                .call()
                .entity(PresupuestoSugeridoResponse.class);
    }

    private ChatClient.ChatClientRequestSpec prompt(UUID idUsuario, List<Message> historial, String mensaje) {
        return intifixChatClient.prompt()
                .messages(historial)
                .user(mensaje)
                .tools(technicianTools, serviceTools, new UserTools(monolithClient, idUsuario));
    }

    static String hilo(UUID idUsuario, String conversationId) {
        return idUsuario + ":" + conversationId;
    }
}
