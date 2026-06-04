package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class TokenExpiredException extends DomainException {
    public TokenExpiredException() {
        super(ErrorCode.AUTH_TOKEN_EXPIRED, "Token has expired");
    }
}
