package ma.noba.shared.domain.exception;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
