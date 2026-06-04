package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

import java.time.Duration;

public class RateLimitExceededException extends DomainException {

    private final Duration retryAfter;

    public RateLimitExceededException(Duration retryAfter) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, "Rate limit exceeded");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}