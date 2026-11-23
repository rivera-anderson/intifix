package com.intifix.chatnotif.notifications.service.impl;

import com.intifix.chatnotif.notifications.dto.response.NotificacionResponse;
import com.intifix.chatnotif.notifications.entity.NotificacionDocument;
import com.intifix.chatnotif.notifications.entity.TipoNotificacion;
import com.intifix.chatnotif.notifications.exception.NotificacionNoEncontradaException;
import com.intifix.chatnotif.notifications.mapper.NotificacionMapper;
import com.intifix.chatnotif.notifications.repository.NotificacionRepository;
import com.intifix.chatnotif.notifications.service.NotificacionService;
import com.intifix.chatnotif.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;

    @Override
    public NotificacionResponse notificar(UUID idDestinatario, TipoNotificacion tipo,
                                          String titulo, String cuerpo, UUID referenciaId) {
        NotificacionDocument doc = NotificacionDocument.builder()
                .id(UUID.randomUUID())
                .idDestinatario(idDestinatario)
                .tipo(tipo)
                .titulo(titulo)
                .cuerpo(cuerpo)
                .referenciaId(referenciaId)
                .leida(false)
                .build();
        NotificacionDocument guardada = notificacionRepository.save(doc);
        log.debug("Notificación {} ({}) creada para {}", guardada.getId(), tipo, idDestinatario);
        return notificacionMapper.toResponse(guardada);
    }

    @Override
    public Page<NotificacionResponse> misNotificaciones(Pageable pageable) {
        UUID userId = SecurityUtils.currentUserId();
        return notificacionRepository.findByIdDestinatario(userId, pageable)
                .map(notificacionMapper::toResponse);
    }

    @Override
    public Page<NotificacionResponse> misNoLeidas(Pageable pageable) {
        UUID userId = SecurityUtils.currentUserId();
        return notificacionRepository.findByIdDestinatarioAndLeida(userId, false, pageable)
                .map(notificacionMapper::toResponse);
    }

    @Override
    public long contarNoLeidas() {
        return notificacionRepository.countByIdDestinatarioAndLeida(SecurityUtils.currentUserId(), false);
    }

    @Override
    public void marcarLeida(UUID idNotificacion) {
        NotificacionDocument doc = cargarPropia(idNotificacion);
        if (!doc.isLeida()) {
            doc.setLeida(true);
            doc.setLeidoEn(Instant.now());
            notificacionRepository.save(doc);
        }
    }

    @Override
    public long marcarTodasLeidas() {
        UUID userId = SecurityUtils.currentUserId();
        List<NotificacionDocument> noLeidas = notificacionRepository.findByIdDestinatarioAndLeida(userId, false);
        Instant ahora = Instant.now();
        noLeidas.forEach(n -> {
            n.setLeida(true);
            n.setLeidoEn(ahora);
        });
        notificacionRepository.saveAll(noLeidas);
        return noLeidas.size();
    }

    @Override
    public void eliminar(UUID idNotificacion) {
        NotificacionDocument doc = cargarPropia(idNotificacion);
        notificacionRepository.delete(doc);
    }

    /**
     * Carga una notificación garantizando que pertenece al usuario autenticado.
     * El acceso ajeno se trata como "no encontrada" para no filtrar existencia.
     */
    private NotificacionDocument cargarPropia(UUID idNotificacion) {
        NotificacionDocument doc = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> NotificacionNoEncontradaException.porId(idNotificacion));
        if (!SecurityUtils.currentUserId().equals(doc.getIdDestinatario())) {
            throw NotificacionNoEncontradaException.porId(idNotificacion);
        }
        return doc;
    }
}
