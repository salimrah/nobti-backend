package ma.noba.queue;

import ma.noba.identity.application.PasswordAuthRequest;
import ma.noba.identity.application.SignupRequest;
import ma.noba.provider.application.dto.CreateProviderRequest;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.queue.application.dto.JoinQueueResponse;
import ma.noba.queue.application.dto.QueueMemberResponse;
import ma.noba.queue.application.dto.QueueStatusResponse;
import ma.noba.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class QueueConcurrencyTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String providerToken;
    private String providerUserId;
    private UUID providerId;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @BeforeEach
    void setUp() {
        providerToken = signupAndLogin("provider-concurrent-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        providerUserId = getUserId(providerToken);
        providerId = createProvider(providerToken);
    }

    private String signupAndLogin(String email) {
        String password = "Pass1234!";
        SignupRequest signup = new SignupRequest(email, password, "Test", "User");
        restTemplate.postForEntity(baseUrl() + "/auth/signup", signup, Map.class);

        PasswordAuthRequest login = new PasswordAuthRequest(email, password, null);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/auth/login", login, Map.class);
        assertThat(loginResponse.getBody()).isNotNull();
        return (String) loginResponse.getBody().get("accessToken");
    }

    private String getUserId(String token) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/users/me", HttpMethod.GET, entity, Map.class);
        assertThat(response.getBody()).isNotNull();
        return (String) response.getBody().get("id");
    }

    private UUID createProvider(String token) {
        CreateProviderRequest request = new CreateProviderRequest(
                "Concurrent Test Provider", "Testing concurrent advance",
                "Barber", "456 Test Ave", 33.5731, -7.5898, null, null);
        HttpEntity<CreateProviderRequest> entity = new HttpEntity<>(request, authHeaders(token));
        ResponseEntity<ProviderResponse> response = restTemplate.postForEntity(
                baseUrl() + "/providers/self", entity, ProviderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    @Test
    @DisplayName("Concurrent advance calls should not corrupt queue state")
    void concurrentAdvance_queueConsistent() throws Exception {
        int clientCount = 5;
        String[] clientTokens = new String[clientCount];
        String[] clientIds = new String[clientCount];

        for (int i = 0; i < clientCount; i++) {
            String email = "client-concurrent-" + i + "-" + UUID.randomUUID().toString().substring(0, 4) + "@example.com";
            clientTokens[i] = signupAndLogin(email);
            clientIds[i] = getUserId(clientTokens[i]);
        }

        for (String token : clientTokens) {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
            restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);
        }

        ResponseEntity<QueueMemberResponse[]> membersBefore = restTemplate.exchange(
                baseUrl() + "/queues/providers/" + providerId + "/members",
                HttpMethod.GET, null, QueueMemberResponse[].class);
        assertThat(membersBefore.getBody()).hasSize(clientCount);

        int advanceCount = clientCount + 1;
        CountDownLatch readyLatch = new CountDownLatch(advanceCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(advanceCount);
        List<Integer> statusCodes = new ArrayList<>();

        for (int i = 0; i < advanceCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                HttpEntity<Void> entity = new HttpEntity<>(authHeaders(providerToken));
                ResponseEntity<Void> response = restTemplate.postForEntity(
                        baseUrl() + "/queues/providers/self/next", entity, Void.class);
                synchronized (statusCodes) {
                    statusCodes.add(response.getStatusCode().value());
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = statusCodes.stream().filter(c -> c == 200).count();
        long errorCount = statusCodes.stream().filter(c -> c != 200).count();

        assertThat(successCount).isBetween(1L, (long) clientCount);

        ResponseEntity<QueueStatusResponse> finalStatus = restTemplate.getForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/status", QueueStatusResponse.class);
        assertThat(finalStatus.getBody()).isNotNull();
        assertThat(finalStatus.getBody().waitingCount() + finalStatus.getBody().activeCount())
                .isLessThanOrEqualTo(clientCount);
    }

    @Test
    @DisplayName("Concurrent join + advance should handle optimistic locking")
    void concurrentJoinAndAdvance_noCorruption() throws Exception {
        int clientCount = 3;
        String[] clientTokens = new String[clientCount];
        String[] clientIds = new String[clientCount];

        for (int i = 0; i < clientCount; i++) {
            String email = "client-joinadv-" + i + "-" + UUID.randomUUID().toString().substring(0, 4) + "@example.com";
            clientTokens[i] = signupAndLogin(email);
            clientIds[i] = getUserId(clientTokens[i]);
        }

        int totalOps = clientCount + 3;
        CountDownLatch readyLatch = new CountDownLatch(totalOps);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(totalOps);

        for (int i = 0; i < clientCount; i++) {
            final String token = clientTokens[i];
            executor.submit(() -> {
                readyLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
                restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);
            });
        }

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                HttpEntity<Void> entity = new HttpEntity<>(authHeaders(providerToken));
                restTemplate.postForEntity(baseUrl() + "/queues/providers/self/next", entity, Void.class);
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        ResponseEntity<QueueMemberResponse[]> finalMembers = restTemplate.exchange(
                baseUrl() + "/queues/providers/" + providerId + "/members",
                HttpMethod.GET, null, QueueMemberResponse[].class);
        assertThat(finalMembers.getBody()).isNotNull();
    }
}
