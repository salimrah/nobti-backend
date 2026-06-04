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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueueIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private UUID providerId;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    @BeforeEach
    void setUp() {
        signupAndLogin();
        createProvider();
    }

    private void signupAndLogin() {
        String email = "queue-test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "QueueTest123!", "Queue", "Tester");
        restTemplate.postForEntity(baseUrl() + "/auth/signup", signupRequest, Map.class);

        PasswordAuthRequest loginRequest = new PasswordAuthRequest(email, "QueueTest123!", null);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/auth/login", loginRequest, Map.class);
        assertThat(loginResponse.getBody()).isNotNull();
        this.accessToken = (String) loginResponse.getBody().get("accessToken");
    }

    private void createProvider() {
        CreateProviderRequest request = new CreateProviderRequest(
                "Test Provider",
                "A test provider for queue integration",
                "Hair Salon",
                "123 Test St",
                33.5731,
                -7.5898,
                null,
                null
        );

        HttpEntity<CreateProviderRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<ProviderResponse> response = restTemplate.postForEntity(baseUrl() + "/providers/self", entity, ProviderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.providerId = response.getBody().id();
    }

    @Test
    @DisplayName("Should get queue status for a provider")
    void getQueueStatus() {
        ResponseEntity<QueueStatusResponse> response = restTemplate.getForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/status",
                QueueStatusResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueueStatusResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.queueId()).isNotNull();
    }

    @Test
    @DisplayName("Should join queue and receive position")
    void joinQueue_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<JoinQueueResponse> response = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/join",
                entity,
                JoinQueueResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JoinQueueResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.position()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get queue members after joining")
    void getQueueMembers_afterJoin() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/members",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should get my position after joining")
    void getMyPosition_afterJoin() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);

        ResponseEntity<QueueMemberResponse> response = restTemplate.exchange(
                baseUrl() + "/queues/providers/" + providerId + "/my-position",
                HttpMethod.GET,
                entity,
                QueueMemberResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueueMemberResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.position()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should leave queue successfully")
    void leaveQueue_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/queues/providers/" + providerId + "/leave",
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should pause queue as provider")
    void pauseQueue() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/self/pause",
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should resume queue after pause")
    void resumeQueue() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        restTemplate.postForEntity(baseUrl() + "/queues/providers/self/pause", entity, Void.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/self/resume",
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should get my queues today (empty initially)")
    void getMyQueuesToday_empty() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        @SuppressWarnings("unchecked")
        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/queues/my/today",
                HttpMethod.GET,
                entity,
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
