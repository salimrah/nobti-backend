package ma.noba.shared.domain.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, path, Instant.now(), null);
    }

    public record FieldError(String field, String message) {}
}
