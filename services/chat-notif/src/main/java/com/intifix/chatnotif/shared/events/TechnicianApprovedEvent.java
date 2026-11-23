package com.intifix.chatnotif.shared.events;

import java.util.UUID;

/**
 * Se publica cuando un administrador aprueba a un técnico.
 */
public record TechnicianApprovedEvent(
        UUID technicianId,
        UUID approvedBy
) {}
