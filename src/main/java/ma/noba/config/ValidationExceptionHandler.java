package ma.noba.config;

import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        return new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                "Validation failed: " + errors.size() + " error(s)",
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(),
                Instant.now(),
                errors
        );
    }

    private ErrorResponse.FieldError toFieldError(FieldError error) {
        return new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage());
    }
}
