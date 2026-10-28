package ma.noba.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ma.noba.identity.infrastructure.HttpSmsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpSmsProviderTest {

    private HttpServer server;
    private int port;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedAuth = new AtomicReference<>();
    private volatile int responseStatus = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sms", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpSmsProvider provider() {
        return new HttpSmsProvider(
                "http://localhost:" + port + "/sms",
                "POST",
                "application/json",
                "Authorization",
                "Bearer test-token",
                "",
                "+212600000000",
                2000);
    }

    @Test
    @DisplayName("Sends a JSON payload with the configured auth header")
    void sendsJsonPayload() throws Exception {
        provider().sendSms("+212700000010", "Your Noba code is 123456");

        assertThat(receivedAuth.get()).isEqualTo("Bearer test-token");
        JsonNode json = new ObjectMapper().readTree(receivedBody.get());
        assertThat(json.get("to").asText()).isEqualTo("+212700000010");
        assertThat(json.get("message").asText()).isEqualTo("Your Noba code is 123456");
        assertThat(json.get("from").asText()).isEqualTo("+212600000000");
    }

    @Test
    @DisplayName("Escapes quotes and newlines so the JSON payload stays valid")
    void escapesUnsafeCharacters() throws Exception {
        provider().sendSms("+212700000010", "Code \"123\"\nDo not share");

        JsonNode json = new ObjectMapper().readTree(receivedBody.get());
        assertThat(json.get("message").asText()).isEqualTo("Code \"123\"\nDo not share");
    }

    @Test
    @DisplayName("A non-2xx gateway response fails the send")
    void non2xxFails() {
        responseStatus = 500;

        assertThatThrownBy(() -> provider().sendSms("+212700000010", "code"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("500");
    }
}
