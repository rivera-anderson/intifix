package com.intifix.ai.tools;

import java.util.Comparator;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.intifix.ai.dto.TecnicoRankeado;
import com.intifix.ai.shared.client.MonolithClient;

import lombok.RequiredArgsConstructor;

/**
 * Tool Calling para recuperar técnicos reales. La búsqueda tolerante de la
 * especialidad y la reputación las resuelve el monolito (dueño de esos datos);
 * aquí solo se ordena para recomendar.
 */
@Component
@RequiredArgsConstructor
public class TechnicianTools {

    private static final int TOP_RECOMENDACIONES = 3;

    private final MonolithClient monolithClient;

    @Tool(description = """
            Busca técnicos aprobados por categoría o especialidad.
            Acepta nombres parciales o coloquiales: "refrigeradoras", "fridge",
            "lavadora", "tele", "luz", "caño", etc.
            Devuelve los técnicos de la categoría más parecida.""")
    public List<TecnicoRankeado> findByCategory(
            @ToolParam(description = "Nombre o palabra clave de la categoría") String category) {
        return monolithClient.tecnicosPorCategoria(category);
    }

    @Tool(description = """
            Devuelve los mejores técnicos para una categoría ordenados por
            calificación promedio (mayor a menor). Úsalo cuando el usuario
            pida recomendaciones o los "mejores" técnicos.
            Acepta nombres parciales: "refrigeradoras", "lavadoras", etc.""")
    public List<TecnicoRankeado> findTopRated(
            @ToolParam(description = "Nombre o palabra clave de la categoría") String category) {
        return monolithClient.tecnicosPorCategoria(category).stream()
                .sorted(Comparator
                        .comparing((TecnicoRankeado t) -> t.disponible() ? 0 : 1)
                        .thenComparing(TecnicoRankeado::promedioCalificacion,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_RECOMENDACIONES)
                .toList();
    }

    @Tool(description = """
            Busca técnicos a partir de la descripción de un problema técnico.
            Infiere la categoría más probable del problema y busca con coincidencia
            parcial (ej: "mi nevera no enfría" → busca "refrigeradoras").""")
    public List<TecnicoRankeado> findByProblem(
            @ToolParam(description = "Descripción del problema o categoría probable") String problem) {
        return monolithClient.tecnicosPorCategoria(problem);
    }
}
