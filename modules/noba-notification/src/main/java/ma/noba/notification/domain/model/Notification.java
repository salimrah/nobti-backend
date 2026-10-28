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

/** One row per logical message; the in-app inbox item. */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationCategory category;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "template_key", length = 80)
    private String templateKey;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(length = 16)
    private String locale;

    @Column(name = "dedupe_key", length = 160)
    private String dedupeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = NotificationStatus.CREATED;
        if (category == null) category = NotificationCategory.TRANSACTIONAL;
        if (priority <= 0) priority = 1;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Notification create(UUID userId,
                                      String type,
                                      NotificationCategory category,
                                      int priority,
                                      String title,
                                      String body,
                                      Map<String, Object> data,
                                      String dedupeKey,
                                      String locale) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = type;
        n.category = category == null ? NotificationCategory.TRANSACTIONAL : category;
        n.priority = priority <= 0 ? 1 : priority;
        n.title = truncate(title, 160);
        n.body = body == null ? "" : body;
        n.data = data;
        n.dedupeKey = dedupeKey;
        n.locale = locale;
        n.status = NotificationStatus.CREATED;
        return n;
    }

    public void markRead(Instant when) {
        if (readAt == null) {
            readAt = when;
        }
    }

    public boolean isUnread() {
        return readAt == null;
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
