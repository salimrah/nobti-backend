package ma.noba.booking.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Appointment {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    private UUID serviceId;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Appointment(UUID userId, UUID providerId, UUID serviceId, Instant startTime, Instant endTime, String notes) {
        this.userId = userId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.PENDING;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        if (status != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Only a pending appointment can be confirmed (was " + status + ")");
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed appointment can be completed (was " + status + ")");
        }
        this.status = AppointmentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (isFinished()) {
            throw new IllegalStateException("A finished appointment cannot be cancelled (was " + status + ")");
        }
        this.status = AppointmentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void markNoShow() {
        if (isFinished()) {
            throw new IllegalStateException("A finished appointment cannot be marked as no-show (was " + status + ")");
        }
        this.status = AppointmentStatus.NO_SHOW;
        this.updatedAt = Instant.now();
    }

    private boolean isFinished() {
        return status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.NO_SHOW;
    }
}
