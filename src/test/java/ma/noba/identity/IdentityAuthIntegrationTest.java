package ma.noba.identity;

import ma.noba.identity.application.AuthToken;
import ma.noba.shared.AbstractIntegrationTest;
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

class IdentityAuthIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String newEmail() {
        return "it-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }

    /** Signs up an account and marks its email verified so password login is permitted. */
    private void signupVerified(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", password, "firstName", "Test", "lastName", "User"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID userId = UUID.fromString((String) response.getBody().get("userId"));
        verifyEmail(userId);
    }

    private AuthToken login(String email, String password) {
        ResponseEntity<AuthToken> response = restTemplate.postForEntity(baseUrl() + "/login",
                Map.of("email", email, "password", password), AuthToken.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    @Test
    @DisplayName("Health endpoint should return OK")
    void healthCheck() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/hello", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("noba-backend is running");
    }

    @Test
    @DisplayName("Signup creates an unverified user")
    void signup_success() {
        String email = newEmail();
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", "TestPass123!", "firstName", "John", "lastName", "Doe"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("email")).isEqualTo(email);
        assertThat(response.getBody().get("userId")).isNotNull();
        assertThat(response.getBody().get("emailVerified")).isEqualTo(false);
    }

    @Test
    @DisplayName("Signup rejects a weak password")
    void signup_weakPassword() {
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", newEmail(), "password", "short", "firstName", "Jane", "lastName", "Doe"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Signup rejects a duplicate email")
    void signup_duplicateEmail() {
        String email = newEmail();
        restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", "DupPass123!", "firstName", "First", "lastName", "User"), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", "DupPass123!", "firstName", "Second", "lastName", "User"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Unverified users cannot log in with a password")
    void login_unverifiedEmailRejected() {
        String email = newEmail();
        restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", "TestPass123!", "firstName", "A", "lastName", "B"), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login",
                Map.of("email", email, "password", "TestPass123!"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("A verified user can log in and receives tokens")
    void login_success() {
        String email = newEmail();
        signupVerified(email, "TestPass123!");

        AuthToken body = login(email, "TestPass123!");
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.expiresIn()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Login rejects a wrong password")
    void login_wrongPassword() {
        String email = newEmail();
        signupVerified(email, "TestPass123!");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login",
                Map.of("email", email, "password", "WrongPass123!"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Login rejects a non-existent email")
    void login_nonExistentEmail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login",
                Map.of("email", newEmail(), "password", "TestPass123!"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("A new user is assigned the USER role")
    void signup_assignsUserRole() {
        String email = newEmail();
        signupVerified(email, "TestPass123!");
        AuthToken tokens = login(email, "TestPass123!");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokens.accessToken());
        ResponseEntity<Map> me = restTemplate.exchange("http://localhost:" + port + "/api/v1/users/me",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat((List<String>) me.getBody().get("roles")).contains("USER");
    }

    @Test
    @DisplayName("Refresh exchanges a refresh token for new tokens")
    void refreshToken_success() {
        String email = newEmail();
        signupVerified(email, "TestPass123!");
        AuthToken tokens = login(email, "TestPass123!");

        ResponseEntity<AuthToken> response = restTemplate.postForEntity(baseUrl() + "/token/refresh",
                Map.of("refreshToken", tokens.refreshToken()), AuthToken.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("Refresh rejects an invalid token")
    void refreshToken_invalid() {
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/token/refresh",
                Map.of("refreshToken", "invalid-token"), Map.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Logout revokes the session")
    void logout_success() {
        String email = newEmail();
        signupVerified(email, "TestPass123!");
        AuthToken tokens = login(email, "TestPass123!");

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/logout",
                Map.of("accessToken", tokens.accessToken(), "refreshToken", tokens.refreshToken()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Forgot-password is accepted for any email (no enumeration)")
    void forgotPassword_success() {
        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/forgot-password",
                Map.of("email", newEmail()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Verify-email rejects an invalid code")
    void verifyEmail_invalidCode() {
        String email = newEmail();
        restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", email, "password", "TestPass123!", "firstName", "T", "lastName", "U"), Map.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/verify-email",
                Map.of("email", email, "code", "invalid-code"), Void.class);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
