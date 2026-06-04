package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class TokenRevokedException extends DomainException {
    public TokenRevokedException() {
        super(ErrorCode.AUTH_TOKEN_INVALID, "Token has been revoked");
    }
}
