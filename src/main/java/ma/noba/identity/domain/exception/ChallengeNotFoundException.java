package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class ChallengeNotFoundException extends DomainException {
    public ChallengeNotFoundException(String message) {
        super(ErrorCode.OTP_CHALLENGE_NOT_FOUND, message);
    }
}
