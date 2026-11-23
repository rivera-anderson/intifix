package com.intifix.chatnotif.shared.events;

import java.util.UUID;

public record CertificadoAprobadoEvent(
        UUID idTecnico,
        UUID idEspecialidad,
        String nombreEspecialidad
) {}
