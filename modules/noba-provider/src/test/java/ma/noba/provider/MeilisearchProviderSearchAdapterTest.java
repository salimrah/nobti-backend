package ma.noba.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.provider.infra.MeilisearchProviderSearchAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeilisearchProviderSearchAdapterTest {

    @Mock ProviderRepository providerRepository;

    private HttpServer server;
    private int port;
    private MeilisearchProviderSearchAdapter adapter;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID providerId = UUID.randomUUID();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().toString());
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            String response = exchange.getRequestURI().getPath().endsWith("/search")
                    ? "{\"hits\":[{\"id\":\"" + providerId + "\"}],\"estimatedTotalHits\":1}"
                    : "{\"taskUid\":1}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
        adapter = new MeilisearchProviderSearchAdapter(
                "http://localhost:" + port, "test-key", "providers", 2000, providerRepository);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private Provider provider(ProviderStatus status) {
        Provider provider = Provider.create(UUID.randomUUID(), "Atlas Barber Club", "Barber");
        provider.setId(providerId);
        provider.setDescription("Premium barber in Casablanca");
        provider.setStatus(status);
        return provider;
    }

    @Test
    @DisplayName("Search sends the query to Meilisearch and returns providers in hit order")
    void searchUsesMeilisearch() throws Exception {
        Provider provider = provider(ProviderStatus.ACTIVE);
        when(providerRepository.findAllById(any())).thenReturn(List.of(provider));

        var page = adapter.search("atlas", PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(provider);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(lastAuth.get()).isEqualTo("Bearer test-key");
        assertThat(lastPath.get()).contains("/indexes/providers/search");

        JsonNode request = objectMapper.readTree(lastBody.get());
        assertThat(request.get("q").asText()).isEqualTo("atlas");
        assertThat(request.get("limit").asInt()).isEqualTo(10);
        assertThat(request.get("offset").asInt()).isZero();
    }

    @Test
    @DisplayName("Indexing sends an ACTIVE provider document")
    void indexesActiveProvider() throws Exception {
        adapter.index(provider(ProviderStatus.ACTIVE));

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).contains("/indexes/providers/documents");
        JsonNode documents = json.readTree(lastBody.get());
        assertThat(documents.isArray()).isTrue();
        assertThat(documents.get(0).get("id").asText()).isEqualTo(providerId.toString());
        assertThat(documents.get(0).get("name").asText()).isEqualTo("Atlas Barber Club");
        assertThat(documents.get(0).get("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("A non-active provider is removed from the index")
    void indexRemovesInactiveProvider() {
        adapter.index(provider(ProviderStatus.INACTIVE));

        assertThat(lastMethod.get()).isEqualTo("DELETE");
        assertThat(lastPath.get()).contains("/indexes/providers/documents/" + providerId);
    }
}
