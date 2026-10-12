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
        ResponseEntity<Map> signupResponse = restTemplate.postForEntity(baseUrl() + "/auth/signup", signupRequest, Map.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verifyEmail(UUID.fromString((String) signupResponse.getBody().get("userId")));

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
        setBusinessHours();
    }

    /** The queue can only be joined while the provider is open, so open all week. */
    private void setBusinessHours() {
        List<Map<String, Object>> days = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            days.add(Map.of("dayOfWeek", day, "opensAt", "00:00", "closesAt", "23:59", "closed", false));
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("days", days), authHeaders());
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/providers/self/hours", HttpMethod.PUT, entity, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should get queue status for a provider")
    void getQueueStatus() {
        // A queue only exists after the first member joins.
        restTemplate.postForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/join",
                new HttpEntity<>(authHeaders()), JoinQueueResponse.class);

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

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/queues/providers/" + providerId + "/members",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
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
    @DisplayName("Marking a member as no-show removes them")
    void markNoShow_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        JoinQueueResponse join = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class).getBody();
        assertThat(join).isNotNull();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/self/members/" + join.memberId() + "/no-show",
                entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should pause queue as provider")
    void pauseQueue() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        // A queue must exist before it can be paused.
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);

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
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/join", entity, JoinQueueResponse.class);
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

    @Test
    @DisplayName("Joining the waitlist records an entry visible to the client")
    void waitlist_joinAndList() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<Map> join = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/waitlist", entity, Map.class);
        assertThat(join.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        @SuppressWarnings("unchecked")
        ResponseEntity<List> mine = restTemplate.exchange(
                baseUrl() + "/queues/waitlist/me", HttpMethod.GET, entity, List.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).hasSize(1);
        assertThat(((Map) mine.getBody().get(0)).get("status")).isEqualTo("WAITING");
    }

    @Test
    @DisplayName("Joining the waitlist twice is rejected")
    void waitlist_duplicateRejected() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/waitlist", entity, Map.class);

        ResponseEntity<Map> second = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/" + providerId + "/waitlist", entity, Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Provider sees their waitlist")
    void providerWaitlist_listsEntries() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/waitlist", entity, Map.class);

        @SuppressWarnings("unchecked")
        ResponseEntity<List> list = restTemplate.exchange(
                baseUrl() + "/queues/providers/self/waitlist", HttpMethod.GET, entity, List.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Resuming a queue notifies waiting clients")
    void resume_notifiesWaitlist() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        restTemplate.postForEntity(baseUrl() + "/queues/providers/" + providerId + "/waitlist", entity, Map.class);

        // Resume both creates/keeps the queue open and notifies the waitlist.
        ResponseEntity<Void> resume = restTemplate.postForEntity(
                baseUrl() + "/queues/providers/self/resume", entity, Void.class);
        assertThat(resume.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        ResponseEntity<List> mine = restTemplate.exchange(
                baseUrl() + "/queues/waitlist/me", HttpMethod.GET, entity, List.class);
        assertThat(mine.getBody()).hasSize(1);
        assertThat(((Map) mine.getBody().get(0)).get("status")).isEqualTo("NOTIFIED");
    }
}
