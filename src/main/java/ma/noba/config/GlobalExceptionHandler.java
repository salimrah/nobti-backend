package ma.noba.config;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorResponse;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Method-level authorization failures (e.g. {@code @PreAuthorize}) are thrown inside the
     * controller invocation, so they reach this advice before Spring Security's
     * ExceptionTranslationFilter. Without an explicit handler they would be caught by the generic
     * handler and surfaced as 500 INTERNAL_ERROR instead of 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "FORBIDDEN",
                "Access denied",
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        captureSentry(ex, SentryLevel.WARNING);
        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Constraint violations (unique/foreign-key/exclusion) mean the request conflicts with existing
     * data — e.g. a double-booked appointment caught by the exclusion constraint. Return 409 rather
     * than exposing a 500.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ErrorResponse response = ErrorResponse.of(
                "CONFLICT",
                "Request conflicts with existing data",
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        captureSentry(ex, SentryLevel.WARNING);
        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        captureSentry(ex, SentryLevel.WARNING);
        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        captureSentry(ex, SentryLevel.ERROR);
        ErrorResponse response = ErrorResponse.of(
                "STATE_ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        captureSentry(ex, SentryLevel.WARNING);
        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Preserve the explicit status carried by {@link ResponseStatusException} (e.g. 403/404)
     * instead of letting the generic handler turn it into a 500.
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
        ErrorResponse response = ErrorResponse.of(
                "REQUEST_ERROR",
                ex.getReason() != null ? ex.getReason() : "Request failed",
                null
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        captureSentry(ex, SentryLevel.WARNING);
        ErrorResponse response = ErrorResponse.of(
                "NOT_FOUND",
                "Resource not found",
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        captureSentry(ex, SentryLevel.ERROR);
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An internal error occurred",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void captureSentry(Exception ex, SentryLevel level) {
        Sentry.withScope(scope -> {
            scope.setLevel(level);
            scope.setTag("error_code", ex instanceof DomainException de ? de.getErrorCode().getCode() : "UNKNOWN");
            String userId = MDC.get("userId");
            if (userId != null) {
                var user = new io.sentry.protocol.User();
                user.setId(userId);
                scope.setUser(user);
            }
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                scope.setTag("correlation_id", correlationId);
            }
            scope.setTag("exception_type", ex.getClass().getSimpleName());
            Sentry.captureException(ex);
        });
    }
}
