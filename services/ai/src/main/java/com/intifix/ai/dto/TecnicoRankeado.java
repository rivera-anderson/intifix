package com.intifix.ai.dto;

import java.math.BigDecimal;

/**
 * Técnico listo para el modelo. Lo arma el monolito (que es dueño de los datos
 * de técnicos y reputación) y llega por la API interna.
 *
 * <p>{@code urlPerfil} viene pre-construida para que GPT-4o nunca tenga que
 * ensamblar un UUID: solo copia el valor al crear el link markdown.</p>
 *
 * @param disponibilidad nombre del enum del monolito: DISPONIBLE, OCUPADO, etc.
 */
public record TecnicoRankeado(
        String urlPerfil,
        String nombre,
        BigDecimal tarifaBase,
        Integer experienciaAnios,
        String disponibilidad,
        BigDecimal promedioCalificacion,
        Integer totalResenas,
        Integer totalServicios
) {
    public boolean disponible() {
        return "DISPONIBLE".equals(disponibilidad);
    }
}
