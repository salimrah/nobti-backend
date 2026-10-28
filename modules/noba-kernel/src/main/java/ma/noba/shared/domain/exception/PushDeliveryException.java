package ma.noba.shared.domain.exception;

/**
 * Provider-neutral push delivery failure. {@code invalidToken} tells the caller to remove the
 * device token (for example FCM {@code UNREGISTERED}).
 */
public class PushDeliveryException extends RuntimeException {

    private final boolean invalidToken;

    public PushDeliveryException(boolean invalidToken, String message, Throwable cause) {
        super(message, cause);
        this.invalidToken = invalidToken;
    }

    public boolean isInvalidToken() {
        return invalidToken;
    }
}
