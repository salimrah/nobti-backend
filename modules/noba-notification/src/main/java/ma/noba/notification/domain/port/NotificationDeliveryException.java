package ma.noba.notification.domain.port;

/** Channel delivery failure, classified so the dispatcher knows whether to retry or suppress. */
public class NotificationDeliveryException extends RuntimeException {

    private final boolean invalidRecipient;
    private final boolean retryable;

    public NotificationDeliveryException(boolean invalidRecipient, boolean retryable,
                                         String message, Throwable cause) {
        super(message, cause);
        this.invalidRecipient = invalidRecipient;
        this.retryable = retryable;
    }

    public boolean isInvalidRecipient() {
        return invalidRecipient;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
