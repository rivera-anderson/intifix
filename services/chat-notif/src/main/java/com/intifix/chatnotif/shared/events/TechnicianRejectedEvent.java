package com.intifix.chatnotif.shared.events;

import java.util.UUID;

public record TechnicianRejectedEvent(
        UUID technicianId,
        UUID rejectedBy
) {}
