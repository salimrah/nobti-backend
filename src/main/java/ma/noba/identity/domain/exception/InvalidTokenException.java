package ma.noba.identity.domain.exception;


public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String msg) {
        super(msg);
    }

    public InvalidTokenException(String msg, Exception e) {
        super(msg, e);
    }

}
