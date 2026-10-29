package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One platform broadcast: audience, copy and delivery counters. */
@Entity
@Table(name = "notification_campaign")
@Getter
@Setter
@NoArgsConstructor
public class NotificationCampaign {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 24)
    private String audience;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients;

    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "SENDING";
    }

    public static NotificationCampaign create(String title, String body, String audience,
                                              int total, UUID createdBy) {
        NotificationCampaign campaign = new NotificationCampaign();
        campaign.title = title;
        campaign.body = body;
        campaign.audience = audience;
        campaign.status = "SENDING";
        campaign.totalRecipients = total;
        campaign.createdBy = createdBy;
        return campaign;
    }

    public void complete(int sent, int failed) {
        this.sentCount = sent;
        this.failedCount = failed;
        this.status = failed == 0 ? "SENT" : sent == 0 ? "FAILED" : "PARTIAL";
    }

    /** Creates a campaign that waits for {@link #scheduledAt} before sending. */
    public static NotificationCampaign scheduled(String title, String body, String audience,
                                                 int total, UUID createdBy, Instant scheduledAt) {
        NotificationCampaign campaign = create(title, body, audience, total, createdBy);
        campaign.status = "SCHEDULED";
        campaign.scheduledAt = scheduledAt;
        return campaign;
    }

    public void begin() {
        this.status = "SENDING";
    }

    public void cancel(UUID actorId) {
        this.status = "CANCELLED";
        this.cancelledAt = Instant.now();
        this.cancelledBy = actorId;
    }

    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }
}
