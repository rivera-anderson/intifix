package com.intifix.chatnotif.notifications.listener;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.intifix.chatnotif.notifications.dto.response.NotificacionResponse;
import com.intifix.chatnotif.notifications.entity.TipoNotificacion;
import com.intifix.chatnotif.notifications.service.NotificacionService;
import com.intifix.chatnotif.shared.events.CertificadoAprobadoEvent;
import com.intifix.chatnotif.shared.events.CertificadoRechazadoEvent;
import com.intifix.chatnotif.shared.events.TechnicianApprovedEvent;
import com.intifix.chatnotif.shared.events.TechnicianRejectedEvent;
import com.intifix.chatnotif.shared.messaging.Eventos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Notificaciones a técnicos a partir de eventos del monolito (módulo
 * technicians). Mismos textos que el listener que existía en el monolito;
 * ahora los eventos llegan por RabbitMQ en vez de ser eventos internos de Spring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TecnicoEventsListener {

    private final NotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = Eventos.Q_TECNICO_APROBADO)
    public void alAprobarTecnico(TechnicianApprovedEvent evento) {
        notificar(evento.technicianId(), "¡Perfil aprobado!",
                "Tu perfil de técnico ha sido aprobado. Ya puedes cotizar servicios.",
                evento.technicianId());
    }

    @RabbitListener(queues = Eventos.Q_TECNICO_RECHAZADO)
    public void alRechazarTecnico(TechnicianRejectedEvent evento) {
        notificar(evento.technicianId(), "Perfil no aprobado",
                "Tu perfil de técnico no fue aprobado. Revisa tus documentos y vuelve a intentarlo.",
                evento.technicianId());
    }

    @RabbitListener(queues = Eventos.Q_CERTIFICADO_APROBADO)
    public void alAprobarCertificado(CertificadoAprobadoEvent evento) {
        notificar(evento.idTecnico(), "Certificado aprobado",
                "Tu certificado de " + evento.nombreEspecialidad()
                        + " fue aprobado. Ya puedes cotizar servicios de esta especialidad.",
                evento.idEspecialidad());
    }

    @RabbitListener(queues = Eventos.Q_CERTIFICADO_RECHAZADO)
    public void alRechazarCertificado(CertificadoRechazadoEvent evento) {
        notificar(evento.idTecnico(), "Certificado rechazado",
                "Tu certificado de " + evento.nombreEspecialidad()
                        + " fue rechazado. Sube un nuevo certificado para que sea revisado.",
                evento.idEspecialidad());
    }

    private void notificar(UUID idTecnico, String titulo, String cuerpo, UUID referenciaId) {
        NotificacionResponse notif = notificacionService.notificar(
                idTecnico, TipoNotificacion.SISTEMA, titulo, cuerpo, referenciaId);
        messagingTemplate.convertAndSendToUser(idTecnico.toString(), "/queue/notifications", notif);
        log.debug("Notificación '{}' enviada al técnico {}", titulo, idTecnico);
    }
}
