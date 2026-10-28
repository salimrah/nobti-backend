package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** Localized copy for a notification type. The body may contain {@code {{placeholders}}}. */
@Entity
@Table(name = "notification_template")
@Getter
@NoArgsConstructor
public class NotificationTemplate {

    @Id
    private UUID id;

    @Column(name = "template_key", nullable = false, length = 80)
    private String templateKey;

    @Column(nullable = false, length = 16)
    private String channel;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(nullable = false)
    private int version;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public static NotificationTemplate of(String templateKey, String locale, int version,
                                          String subject, String body) {
        NotificationTemplate template = new NotificationTemplate();
        template.id = UUID.randomUUID();
        template.templateKey = templateKey;
        template.channel = "ANY";
        template.locale = locale;
        template.version = version;
        template.subject = subject;
        template.body = body;
        template.active = true;
        template.createdAt = Instant.now();
        return template;
    }

    /** A new version of the same key + locale, owning the copy (activation is a separate step). */
    public static NotificationTemplate version(String templateKey, String locale, int version,
                                               String subject, String body) {
        return of(templateKey, locale, version, subject, body);
    }
}
