package ma.noba.shared.domain.exception;

public class BusinessException extends DomainException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
