package ma.noba.notification.domain.model;

public enum NotificationEventType {
    CREATED,
    QUEUED,
    SENT,
    DELIVERED,
    FAILED,
    READ,
    SUPPRESSED
}
