package com.intifix.chatnotif.chat.mapper;

import com.intifix.chatnotif.chat.dto.response.ConversacionResponse;
import com.intifix.chatnotif.chat.entity.ConversacionDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversacionMapper {

    // noLeidos depende del usuario que consulta: lo resuelve el servicio.
    @Mapping(target = "noLeidos", ignore = true)
    ConversacionResponse toResponse(ConversacionDocument document);

    ConversacionResponse.UltimoMensajeResponse toUltimoMensajeResponse(ConversacionDocument.UltimoMensaje ultimoMensaje);
}
