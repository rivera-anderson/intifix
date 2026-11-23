package com.intifix.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.intifix.ai.dto.TecnicoRankeado;
import com.intifix.ai.shared.client.MonolithClient;

class TechnicianToolsTest {

    private static TecnicoRankeado tecnico(String nombre, String disponibilidad, String promedio) {
        return new TecnicoRankeado("/cliente/tecnicos/" + nombre, nombre, BigDecimal.TEN, 5,
                disponibilidad, new BigDecimal(promedio), 10, 20);
    }

    @Test
    void topRatedPrioriza_disponibles_y_luego_mejor_calificacion() {
        MonolithClient client = mock(MonolithClient.class);
        when(client.tecnicosPorCategoria("lavadoras")).thenReturn(List.of(
                tecnico("ana", "OCUPADO", "5.0"),
                tecnico("beto", "DISPONIBLE", "4.1"),
                tecnico("caro", "DISPONIBLE", "4.8"),
                tecnico("dani", "DISPONIBLE", "3.0")));

        List<TecnicoRankeado> top = new TechnicianTools(client).findTopRated("lavadoras");

        assertEquals(List.of("caro", "beto", "dani"), top.stream().map(TecnicoRankeado::nombre).toList());
    }
}
