package com.intifix.chatnotif.shared.events;

import java.util.UUID;

/** Un usuario abrió o cerró su sesión WebSocket del chat. */
public record ChatSessionEvent(UUID userId) {}
