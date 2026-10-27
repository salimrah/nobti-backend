package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class InvalidCodeException extends DomainException {
    public InvalidCodeException(String message) {
        super(ErrorCode.OTP_INVALID_CODE, message);
    }
}
