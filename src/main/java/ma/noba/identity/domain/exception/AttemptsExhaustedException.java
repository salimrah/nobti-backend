package ma.noba.identity.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AttemptsExhaustedException extends RuntimeException {
    public AttemptsExhaustedException(String message) {
        super(message);
    }
}
