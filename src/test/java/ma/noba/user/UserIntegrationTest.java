package ma.noba.user;

import ma.noba.identity.application.PasswordAuthRequest;
import ma.noba.identity.application.SignupRequest;
import ma.noba.shared.AbstractIntegrationTest;
import ma.noba.user.application.dto.FcmTokenRequest;
import ma.noba.user.application.dto.UpdateProfileRequest;
import ma.noba.user.application.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private UUID userId;

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
    }

    private void signupAndLogin() {
        String email = "user-test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "UserTest123!", "User", "Tester");
        ResponseEntity<Map> signupResponse = restTemplate.postForEntity(baseUrl() + "/auth/signup", signupRequest, Map.class);
        this.userId = UUID.fromString((String) signupResponse.getBody().get("userId"));

        PasswordAuthRequest loginRequest = new PasswordAuthRequest(email, "UserTest123!", null);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/auth/login", loginRequest, Map.class);
        assertThat(loginResponse.getBody()).isNotNull();
        this.accessToken = (String) loginResponse.getBody().get("accessToken");
    }

    @Test
    @DisplayName("Should get current user profile")
    void getMe() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl() + "/users/me",
                HttpMethod.GET,
                entity,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(userId);
        assertThat(body.email()).startsWith("user-test-");
    }

    @Test
    @DisplayName("Should update user profile")
    void updateProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated", "Name", "https://example.com/pic.jpg", "fr");

        HttpEntity<UpdateProfileRequest> entity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl() + "/users/me/profile",
                HttpMethod.PUT,
                entity,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.firstName()).isEqualTo("Updated");
        assertThat(body.lastName()).isEqualTo("Name");
        assertThat(body.profilePictureUrl()).isEqualTo("https://example.com/pic.jpg");
    }

    @Test
    @DisplayName("Should save FCM token")
    void saveFcmToken() {
        FcmTokenRequest request = new FcmTokenRequest("test-fcm-token-123", "android");

        HttpEntity<FcmTokenRequest> entity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/users/me/device",
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should delete FCM token")
    void deleteFcmToken() {
        FcmTokenRequest request = new FcmTokenRequest("test-fcm-token-456", "ios");
        HttpEntity<FcmTokenRequest> saveEntity = new HttpEntity<>(request, authHeaders());
        restTemplate.postForEntity(baseUrl() + "/users/me/device", saveEntity, Void.class);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/users/me/device?fcmToken=test-fcm-token-456",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should get user by ID")
    void getUserById() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl() + "/users/" + userId,
                HttpMethod.GET,
                entity,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(userId);
    }
}
