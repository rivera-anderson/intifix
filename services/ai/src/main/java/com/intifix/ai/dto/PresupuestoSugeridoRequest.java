package com.intifix.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresupuestoSugeridoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 2000) String descripcion,
        @Size(max = 100) String especialidad
) {}
