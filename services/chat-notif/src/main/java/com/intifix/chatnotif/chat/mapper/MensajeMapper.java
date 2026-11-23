package com.intifix.chatnotif.chat.mapper;

import com.intifix.chatnotif.chat.dto.response.AdjuntoResponse;
import com.intifix.chatnotif.chat.dto.response.MensajeResponse;
import com.intifix.chatnotif.chat.entity.MensajeDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MensajeMapper {

    MensajeResponse toResponse(MensajeDocument document);

    AdjuntoResponse toAdjuntoResponse(MensajeDocument.Adjunto adjunto);
}
