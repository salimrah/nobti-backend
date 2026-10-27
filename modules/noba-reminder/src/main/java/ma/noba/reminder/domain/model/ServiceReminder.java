package ma.noba.reminder.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "service_reminder", indexes = {
    @Index(name = "idx_reminder_next", columnList = "nextReminderAt", unique = false),
    @Index(name = "idx_reminder_user", columnList = "userId", unique = false)
})
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ServiceReminder {

    @Id @GeneratedValue @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    private UUID serviceId;

    @Column(nullable = false, length = 200)
    private String providerName;

    @Column(nullable = false, length = 200)
    private String serviceName;

    @Column(nullable = false)
    private Integer intervalDays;

    @Column(nullable = false)
    private Instant nextReminderAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public ServiceReminder(UUID userId, UUID providerId, UUID serviceId,
                          String providerName, String serviceName,
                          Integer intervalDays) {
        this.userId = userId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.providerName = providerName;
        this.serviceName = serviceName;
        this.intervalDays = intervalDays;
        this.nextReminderAt = Instant.now().plus(intervalDays, ChronoUnit.DAYS);
        this.active = true;
    }

    public void snooze() {
        this.nextReminderAt = Instant.now().plus(this.intervalDays, ChronoUnit.DAYS);
    }

    public void updateInterval(Integer intervalDays) {
        if (intervalDays != null) {
            this.intervalDays = intervalDays;
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
