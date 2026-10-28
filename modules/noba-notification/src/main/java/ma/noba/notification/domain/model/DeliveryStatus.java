package ma.noba.notification.domain.model;

public enum DeliveryStatus {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    SUPPRESSED;

    public boolean terminal() {
        return this == SENT || this == DELIVERED || this == FAILED || this == SUPPRESSED;
    }
}
