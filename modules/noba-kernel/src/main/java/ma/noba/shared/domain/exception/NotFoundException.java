package ma.noba.shared.domain.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, String identifier) {
        super(ErrorCode.NOT_FOUND, "%s with identifier '%s' not found".formatted(resource, identifier));
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
