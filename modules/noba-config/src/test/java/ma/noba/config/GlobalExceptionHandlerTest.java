package ma.noba.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Method-level authorization failures map to 403, not 500")
    void accessDeniedMapsTo403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("Malformed path variables map to 400, not 500")
    void malformedPathVariablesMapTo400() {
        var response = handler.handleTypeMismatch(new MethodArgumentTypeMismatchException(
                "categories", UUID.class, "id", null, new IllegalArgumentException("Invalid UUID")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("ResponseStatusException keeps its declared status code")
    void responseStatusIsPreserved() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("No provider profile");
    }
}
