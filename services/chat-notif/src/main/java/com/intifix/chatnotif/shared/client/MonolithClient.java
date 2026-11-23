package com.intifix.chatnotif.shared.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de la API interna del monolito ({@code /internal/v1/**}).
 *
 * <p>Los datos de usuarios y servicios viven en el Postgres del monolito; este
 * servicio no lee esa base, la consulta por HTTP. Esas rutas no pasan por el
 * gateway (no son públicas) y se protegen con una API key compartida.</p>
 *
 * <p>El estado de la cuenta se cachea unos segundos para no llamar al monolito
 * en cada request, a cambio de que un baneo tarde como máximo ese tiempo en
 * aplicar aquí.</p>
 */
@Slf4j
@Component
public class MonolithClient {

    public static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final Duration estadoTtl;
    private final Map<UUID, CachedEstado> estadoCache = new ConcurrentHashMap<>();

    public MonolithClient(
            @Value("${intifix.monolith.url}") String baseUrl,
            @Value("${intifix.monolith.api-key}") String apiKey,
            @Value("${intifix.monolith.estado-cache-ttl:30s}") Duration estadoTtl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
        this.estadoTtl = estadoTtl;
    }

    public record EstadoUsuario(boolean existe, boolean activo) {}

    public record ServicioParticipantes(UUID idServicio, UUID idCliente, UUID idTecnico) {}

    /**
     * Estado de la cuenta. Si el monolito no responde se devuelve vacío y el
     * llamador decide (el filtro JWT rechaza la request: falla cerrado).
     */
    public Optional<EstadoUsuario> estadoUsuario(UUID idUsuario) {
        CachedEstado cached = estadoCache.get(idUsuario);
        if (cached != null && cached.vigente()) {
            return Optional.of(cached.estado());
        }
        try {
            EstadoUsuario estado = restClient.get()
                    .uri("/internal/v1/usuarios/{id}/estado", idUsuario)
                    .retrieve()
                    .body(EstadoUsuario.class);
            if (estado != null) {
                estadoCache.put(idUsuario, new CachedEstado(estado, Instant.now().plus(estadoTtl)));
            }
            return Optional.ofNullable(estado);
        } catch (RestClientException e) {
            log.error("No se pudo consultar el estado del usuario {} en el monolito: {}", idUsuario, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ServicioParticipantes> participantes(UUID idServicio) {
        try {
            // 404 = el servicio no existe: vacío, no es un error.
            return restClient.get()
                    .uri("/internal/v1/servicios/{id}/participantes", idServicio)
                    .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                            ? Optional.ofNullable(response.bodyTo(ServicioParticipantes.class))
                            : Optional.<ServicioParticipantes>empty());
        } catch (RestClientException e) {
            log.error("No se pudieron obtener los participantes del servicio {}: {}", idServicio, e.getMessage());
            return Optional.empty();
        }
    }

    private record CachedEstado(EstadoUsuario estado, Instant expira) {
        boolean vigente() {
            return Instant.now().isBefore(expira);
        }
    }
}
