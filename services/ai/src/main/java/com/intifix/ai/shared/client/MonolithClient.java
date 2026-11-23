package com.intifix.ai.shared.client;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.intifix.ai.dto.TecnicoRankeado;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de la API interna del monolito ({@code /internal/v1/**}).
 *
 * <p>Técnicos, servicios y clientes viven en el Postgres del monolito; este
 * servicio no lee esa base, la consulta por HTTP con una API key compartida.
 * Esas rutas no pasan por el gateway.</p>
 *
 * <p>Las consultas que usan las tools de la IA devuelven vacío ante error: el
 * modelo responde "no encontré técnicos" en vez de que la conversación falle.</p>
 */
@Slf4j
@Component
public class MonolithClient {

    public static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private static final ParameterizedTypeReference<List<String>> LISTA_TEXTO = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<JsonNode>> LISTA_JSON = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<TecnicoRankeado>> LISTA_TECNICOS = new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final Duration estadoTtl;
    private final Map<UUID, CachedEstado> estadoCache = new ConcurrentHashMap<>();

    public MonolithClient(
            @Value("${intifix.monolith.url}") String baseUrl,
            @Value("${intifix.monolith.api-key}") String apiKey,
            @Value("${intifix.monolith.estado-cache-ttl:30s}") Duration estadoTtl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
        this.estadoTtl = estadoTtl;
    }

    public record EstadoUsuario(boolean existe, boolean activo) {}

    // ------------------------------------------------------------ seguridad

    /** Estado de la cuenta (cacheado). Vacío si el monolito no responde: el filtro JWT niega el acceso. */
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

    // ------------------------------------------------------------ catálogo (tools)

    public List<String> categorias() {
        return lista("/internal/v1/catalogo/categorias", LISTA_TEXTO);
    }

    public List<TecnicoRankeado> tecnicosPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return List.of();
        }
        return lista("/internal/v1/catalogo/tecnicos?categoria={c}", LISTA_TECNICOS, categoria);
    }

    public List<JsonNode> serviciosPorTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return List.of();
        }
        return lista("/internal/v1/catalogo/servicios?titulo={t}", LISTA_JSON, titulo);
    }

    // ------------------------------------------------------------ cliente (tools)

    public JsonNode cliente(UUID idCliente) {
        try {
            return restClient.get()
                    .uri("/internal/v1/clientes/{id}", idCliente)
                    .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                            ? response.bodyTo(JsonNode.class)
                            : null);
        } catch (RestClientException e) {
            log.warn("No se pudo obtener el cliente {}: {}", idCliente, e.getMessage());
            return null;
        }
    }

    public List<JsonNode> serviciosDeCliente(UUID idCliente) {
        return lista("/internal/v1/clientes/{id}/servicios", LISTA_JSON, idCliente);
    }

    // ------------------------------------------------------------ helpers

    private <T> List<T> lista(String uri, ParameterizedTypeReference<List<T>> tipo, Object... vars) {
        try {
            List<T> resultado = restClient.get().uri(uri, vars).retrieve().body(tipo);
            return resultado != null ? resultado : List.of();
        } catch (RestClientException e) {
            log.warn("Error consultando {} en el monolito: {}", uri, e.getMessage());
            return List.of();
        }
    }

    private record CachedEstado(EstadoUsuario estado, Instant expira) {
        boolean vigente() {
            return Instant.now().isBefore(expira);
        }
    }
}
