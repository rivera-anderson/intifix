package com.intifix.ai.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.intifix.ai.shared.client.MonolithClient;

import lombok.RequiredArgsConstructor;

/**
 * Herramientas para consultar servicios y especialidades del marketplace.
 */
@Component
@RequiredArgsConstructor
public class ServiceTools {

    private final MonolithClient monolithClient;

    @Tool(description = """
            Lista ÚNICAMENTE las categorías que tienen técnicos aprobados disponibles
            en este momento. Úsala cuando el usuario pregunte qué categorías hay,
            qué servicios ofrece la plataforma, o cuando una búsqueda devuelve vacío
            y quieres sugerir alternativas con técnicos reales.""")
    public List<String> listAllCategories() {
        return monolithClient.categorias();
    }

    @Tool(description = """
            Busca servicios por tipo o palabra clave en el título
            (ej: "instalación", "reparación de fugas").""")
    public List<JsonNode> getServicesByType(
            @ToolParam(description = "Tipo o palabra clave del servicio") String type) {
        return monolithClient.serviciosPorTitulo(type);
    }
}
