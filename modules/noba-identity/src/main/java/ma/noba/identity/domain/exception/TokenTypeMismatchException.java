package ma.noba.identity.domain.exception;

import ma.noba.shared.domain.exception.DomainException;
import ma.noba.shared.domain.exception.ErrorCode;

public class TokenTypeMismatchException extends DomainException {
    public TokenTypeMismatchException() {
        super(ErrorCode.AUTH_TOKEN_INVALID, "Token type mismatch");
    }
}
