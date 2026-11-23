package com.intifix.chatnotif.shared.messaging;

/**
 * Contrato de mensajería entre servicios (ver docs/eventos.md en la raíz del
 * monorepo). Todos los eventos viajan como JSON por el exchange topic
 * {@value #EXCHANGE}. Los nombres aquí deben coincidir con los del monolito.
 */
public final class Eventos {

    private Eventos() {
    }

    public static final String EXCHANGE = "intifix.events";
    public static final String DEAD_LETTER_EXCHANGE = "intifix.events.dlx";

    // --- Publica el monolito, consume este servicio ---
    public static final String TECNICO_APROBADO = "technician.approved";
    public static final String TECNICO_RECHAZADO = "technician.rejected";
    public static final String CERTIFICADO_APROBADO = "certificate.approved";
    public static final String CERTIFICADO_RECHAZADO = "certificate.rejected";

    // --- Publica este servicio, consume el monolito (auditoría) ---
    public static final String CHAT_MENSAJE_ENVIADO = "chat.message.sent";
    public static final String CHAT_SESION_CONECTADA = "chat.session.connected";
    public static final String CHAT_SESION_DESCONECTADA = "chat.session.disconnected";

    // --- Colas propias de este servicio ---
    public static final String Q_TECNICO_APROBADO = "chat-notif.technician.approved";
    public static final String Q_TECNICO_RECHAZADO = "chat-notif.technician.rejected";
    public static final String Q_CERTIFICADO_APROBADO = "chat-notif.certificate.approved";
    public static final String Q_CERTIFICADO_RECHAZADO = "chat-notif.certificate.rejected";
    public static final String Q_DEAD_LETTER = "chat-notif.dead-letter";
}
