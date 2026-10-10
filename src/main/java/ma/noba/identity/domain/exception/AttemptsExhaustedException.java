package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class AttemptsExhaustedException extends DomainException {
    public AttemptsExhaustedException(String message) {
        super(ErrorCode.OTP_ATTEMPTS_EXHAUSTED, message);
    }
}
