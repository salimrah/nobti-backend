package ma.noba.identity;

import ma.noba.shared.AbstractIntegrationTest;
import ma.noba.identity.application.AuthToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAuthIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/auth";
    }

    @Test
    @DisplayName("Health endpoint should return OK")
    void healthCheck() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/hello", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("noba-backend is running");
    }

    @Test
    @DisplayName("Should signup with valid email and password")
    void signup_success() {
        Map<String, String> request = Map.of(
                "email", "v2test@example.com",
                "password", "TestPass123!",
                "firstName", "John",
                "lastName", "Doe"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("email")).isEqualTo("v2test@example.com");
        assertThat(body.get("userId")).isNotNull();
        assertThat(body.get("emailVerified")).isEqualTo(false);
    }

    @Test
    @DisplayName("Should reject signup with weak password")
    void signup_weakPassword() {
        Map<String, String> request = Map.of(
                "email", "weak@example.com",
                "password", "short",
                "firstName", "Jane",
                "lastName", "Doe"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("After signup, user should have USER role")
    void signup_assignsUserRole() {
        String email = "role-test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String apiBase = "http://localhost:" + port + "/api/v1";

        restTemplate.postForEntity(apiBase + "/auth/signup",
                Map.of("email", email, "password", "TestPass123!"), Map.class);

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(apiBase + "/auth/login",
                Map.of("email", email, "password", "TestPass123!"), Map.class);
        assertThat(loginResponse.getBody()).isNotNull();
        String accessToken = (String) loginResponse.getBody().get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                apiBase + "/users/me", HttpMethod.GET, entity, Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        Set<String> roles = (Set<String>) meResponse.getBody().get("roles");
        assertThat(roles).contains("USER");
    }

    @Test
    @DisplayName("Should reject signup with duplicate email")
    void signup_duplicateEmail() {
        Map<String, String> first = Map.of(
                "email", "dup@example.com",
                "password", "DupPass123!",
                "firstName", "First",
                "lastName", "User"
        );
        restTemplate.postForEntity(baseUrl() + "/signup", first, Map.class);

        Map<String, String> request = Map.of(
                "email", "dup@example.com",
                "password", "DupPass123!",
                "firstName", "Second",
                "lastName", "User"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/signup", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should login with valid credentials and return tokens")
    void login_success() {
        signup_success();

        Map<String, String> request = Map.of(
                "email", "v2test@example.com",
                "password", "TestPass123!"
        );

        ResponseEntity<AuthToken> response = restTemplate.postForEntity(baseUrl() + "/login", request, AuthToken.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthToken body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.expiresIn()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void login_wrongPassword() {
        signup_success();

        Map<String, String> request = Map.of(
                "email", "v2test@example.com",
                "password", "WrongPass123!"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void login_nonExistentEmail() {
        Map<String, String> request = Map.of(
                "email", "nonexistent@example.com",
                "password", "TestPass123!"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should refresh access token with valid refresh token")
    void refreshToken_success() {
        signup_success();

        Map<String, String> loginRequest = Map.of(
                "email", "v2test@example.com",
                "password", "TestPass123!"
        );
        AuthToken loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, AuthToken.class).getBody();
        assertThat(loginResponse).isNotNull();

        Map<String, String> refreshRequest = Map.of("refreshToken", loginResponse.refreshToken());

        ResponseEntity<AuthToken> response = restTemplate.postForEntity(baseUrl() + "/token/refresh", refreshRequest, AuthToken.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthToken body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should reject refresh with invalid token")
    void refreshToken_invalid() {
        Map<String, String> request = Map.of("refreshToken", "invalid-token");

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/token/refresh", request, Map.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should logout and revoke tokens")
    void logout_success() {
        signup_success();

        Map<String, String> loginRequest = Map.of(
                "email", "v2test@example.com",
                "password", "TestPass123!"
        );
        AuthToken loginResponse = restTemplate.postForEntity(baseUrl() + "/login", loginRequest, AuthToken.class).getBody();
        assertThat(loginResponse).isNotNull();

        Map<String, String> logoutRequest = Map.of(
                "accessToken", loginResponse.accessToken(),
                "refreshToken", loginResponse.refreshToken()
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/logout", logoutRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should request password reset for existing user")
    void forgotPassword_success() {
        signup_success();

        Map<String, String> request = Map.of("email", "v2test@example.com");

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/forgot-password", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Should verify email with valid code")
    void verifyEmail_success() {
        signup_success();

        var repo = restTemplate.getRestTemplate();
        ResponseEntity<Map> signupResponse = restTemplate.postForEntity(baseUrl() + "/signup",
                Map.of("email", "verify-email-test@example.com", "password", "TestPass123!", "firstName", "Test", "lastName", "User"),
                Map.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, String> request = Map.of(
                "email", "verify-email-test@example.com",
                "code", "test-code"
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/verify-email", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should resend verification code")
    void resendVerification_success() {
        Map<String, String> request = Map.of(
                "email", "resend-test@example.com",
                "password", "TestPass123!",
                "firstName", "Resend",
                "lastName", "User"
        );
        restTemplate.postForEntity(baseUrl() + "/signup", request, Map.class);

        Map<String, String> resendRequest = Map.of("email", "resend-test@example.com");

        ResponseEntity<Void> response = restTemplate.postForEntity(baseUrl() + "/resend-verification", resendRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }
}
