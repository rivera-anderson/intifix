package com.intifix.chatnotif.shared.events;

import java.util.UUID;

public record CertificadoRechazadoEvent(
        UUID idTecnico,
        UUID idEspecialidad,
        String nombreEspecialidad
) {}
