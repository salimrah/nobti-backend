package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only audit/analytics event for a notification or a delivery attempt. */
@Entity
@Table(name = "notification_event")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private NotificationEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public static NotificationEvent of(UUID notificationId, UUID deliveryId,
                                       NotificationEventType eventType, Map<String, Object> metadata) {
        NotificationEvent e = new NotificationEvent();
        e.notificationId = notificationId;
        e.deliveryId = deliveryId;
        e.eventType = eventType;
        e.metadata = metadata;
        e.occurredAt = Instant.now();
        return e;
    }
}
