package com.intifix.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AiAssistantServiceImplTest {

    @Test
    void elMismoConversationIdDeDosUsuariosSonHilosDistintos() {
        String conversationId = "conv-123";
        assertNotEquals(
                AiAssistantServiceImpl.hilo(UUID.randomUUID(), conversationId),
                AiAssistantServiceImpl.hilo(UUID.randomUUID(), conversationId));
    }
}
