package com.intifix.chatnotif.chat.service;

import com.intifix.chatnotif.chat.dto.request.CrearConversacionRequest;
import com.intifix.chatnotif.chat.dto.request.CrearConsultaRequest;
import com.intifix.chatnotif.chat.dto.response.ConversacionResponse;
import com.intifix.chatnotif.chat.entity.ConversacionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConversacionService {

    ConversacionResponse crear(CrearConversacionRequest request);

    /**
     * Crea (o devuelve la existente) conversación de consulta previa entre el
     * cliente autenticado y el técnico indicado (sin servicio vinculado).
     */
    ConversacionResponse crearConsulta(CrearConsultaRequest request);

    Page<ConversacionResponse> misConversaciones(Pageable pageable);

    ConversacionResponse obtenerPorId(UUID idConversacion);

    void archivar(UUID idConversacion);

    void desarchivar(UUID idConversacion);

    void bloquear(UUID idConversacion);

    void desbloquear(UUID idConversacion);

    void eliminar(UUID idConversacion);

    /**
     * Carga la conversación verificando que {@code idUsuario} sea participante
     * (anti-IDOR). Uso interno del módulo (p.ej. el servicio de mensajes).
     */
    ConversacionDocument cargarParticipando(UUID idConversacion, UUID idUsuario);
}
