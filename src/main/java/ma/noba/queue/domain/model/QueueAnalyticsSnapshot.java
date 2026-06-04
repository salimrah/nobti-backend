package ma.noba.queue.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "queue_analytics_snapshot", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QueueAnalyticsSnapshot {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int totalServed;

    @Column(nullable = false)
    private double avgWaitTimeMinutes;

    @Column(nullable = false)
    private double avgServiceDurationMinutes;

    private int peakHour;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public QueueAnalyticsSnapshot(UUID providerId, LocalDate date) {
        this.providerId = providerId;
        this.date = date;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
