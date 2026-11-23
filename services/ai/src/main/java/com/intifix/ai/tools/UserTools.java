package com.intifix.ai.tools;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.intifix.ai.shared.client.MonolithClient;

/**
 * Datos del cliente que está chateando.
 *
 * <p>Se crea una instancia POR PETICIÓN, ligada al usuario del JWT. Las tools
 * no reciben un id como parámetro: así el modelo no puede (ni por error ni por
 * prompt injection) consultar los datos de otro cliente. En el monolito estas
 * tools aceptaban cualquier UUID.</p>
 */
public class UserTools {

    private final MonolithClient monolithClient;
    private final UUID idUsuario;

    public UserTools(MonolithClient monolithClient, UUID idUsuario) {
        this.monolithClient = monolithClient;
        this.idUsuario = idUsuario;
    }

    @Tool(description = "Obtiene la información básica del cliente que está conversando contigo.")
    public JsonNode getUserInfo() {
        return monolithClient.cliente(idUsuario);
    }

    @Tool(description = "Obtiene el historial reciente de servicios solicitados por el cliente que está conversando contigo.")
    public List<JsonNode> getHistory() {
        return monolithClient.serviciosDeCliente(idUsuario);
    }
}
