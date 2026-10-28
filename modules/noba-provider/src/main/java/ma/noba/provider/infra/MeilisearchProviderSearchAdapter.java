package ma.noba.provider.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.provider.domain.port.ProviderSearchIndexPort;
import ma.noba.provider.domain.port.ProviderSearchPort;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Meilisearch adapter for provider search. Selected with {@code search.provider=meilisearch}.
 *
 * <p>Documents are indexed on provider events (see {@link ProviderSearchIndexListener}) and
 * bootstrapped once at startup.
 */
@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "meilisearch")
public class MeilisearchProviderSearchAdapter implements ProviderSearchPort, ProviderSearchIndexPort {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchProviderSearchAdapter.class);

    private final RestClient client;
    private final String index;
    private final String apiKey;
    private final ProviderRepository providerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MeilisearchProviderSearchAdapter(
            @Value("${search.meilisearch.url:}") String url,
            @Value("${search.meilisearch.api-key:}") String apiKey,
            @Value("${search.meilisearch.index:providers}") String index,
            @Value("${search.meilisearch.timeout-ms:3000}") int timeoutMs,
            ProviderRepository providerRepository) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "search.meilisearch.url is required when SEARCH_PROVIDER=meilisearch");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.index = index;
        this.apiKey = apiKey;
        this.providerRepository = providerRepository;
    }

    @Override
    public Page<Provider> search(String query, Pageable pageable) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("q", query == null ? "" : query);
        request.put("limit", pageable.getPageSize());
        request.put("offset", pageable.getOffset());

        JsonNode response = client.post()
                .uri("/indexes/{index}/search", index)
                .headers(headers -> {
                    if (hasKey()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        List<UUID> ids = new ArrayList<>();
        if (response != null) {
            response.path("hits").forEach(hit -> {
                String id = hit.path("id").asText(null);
                if (id != null) {
                    try {
                        ids.add(UUID.fromString(id));
                    } catch (IllegalArgumentException ignored) {
                        // ignore malformed ids from a stale index
                    }
                }
            });
        }

        Map<UUID, Provider> byId = providerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Provider::getId, Function.identity(), (a, b) -> a));
        List<Provider> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        long total = response != null ? response.path("estimatedTotalHits").asLong(ordered.size()) : ordered.size();
        return new PageImpl<>(ordered, pageable, total);
    }

    @Override
    public void index(Provider provider) {
        if (provider == null || provider.getId() == null) {
            return;
        }
        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            remove(provider.getId());
            return;
        }
        putDocuments(List.of(toDocument(provider)));
    }

    @Override
    public void remove(UUID providerId) {
        if (providerId == null) {
            return;
        }
        client.delete()
                .uri("/indexes/{index}/documents/{id}", index, providerId.toString())
                .headers(headers -> {
                    if (hasKey()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void reindexAll() {
        List<Provider> active = providerRepository.findAllActive();
        if (active.isEmpty()) {
            return;
        }
        putDocuments(active.stream().map(this::toDocument).toList());
        log.info("Reindexed {} providers into Meilisearch", active.size());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIndex() {
        try {
            reindexAll();
        } catch (Exception e) {
            log.warn("Meilisearch bootstrap failed; search will use the current index: {}", e.getMessage());
        }
    }

    private void putDocuments(List<Map<String, Object>> documents) {
        if (documents.isEmpty()) {
            return;
        }
        client.post()
                .uri("/indexes/{index}/documents?primaryKey=id", index)
                .headers(headers -> {
                    if (hasKey()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(documents)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> toDocument(Provider provider) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", provider.getId().toString());
        doc.put("name", provider.getName());
        doc.put("description", provider.getDescription() == null ? "" : provider.getDescription());
        doc.put("category", provider.getCategory() == null ? "" : provider.getCategory());
        doc.put("status", provider.getStatus() != null ? provider.getStatus().name() : null);
        return doc;
    }

    private boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
