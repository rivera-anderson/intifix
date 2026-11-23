package com.intifix.chatnotif.notifications.mapper;

import com.intifix.chatnotif.notifications.dto.response.NotificacionResponse;
import com.intifix.chatnotif.notifications.entity.NotificacionDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionMapper {

    NotificacionResponse toResponse(NotificacionDocument document);
}
