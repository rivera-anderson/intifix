package com.intifix.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Petición de chat hacia el motor de IA.
 */
@Data
public class ChatRequest {

    /**
     * Hilo de conversación. Se guarda siempre ligado al usuario del token, así
     * que un usuario no puede leer ni borrar el hilo de otro aunque conozca su id.
     */
    @NotBlank
    @Size(max = 100)
    private String conversationId;

    /**
     * Ignorado. Se mantiene para no romper a los clientes que aún lo envían:
     * el usuario siempre se toma del JWT.
     */
    @Deprecated
    private String userId;

    @NotBlank
    @Size(max = 4000)
    private String message;
}
