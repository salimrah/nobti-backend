package ma.noba.booking.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_slot")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AvailabilitySlot {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    private Integer dayOfWeek;

    private LocalDate specificDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    public AvailabilitySlot(UUID providerId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.providerId = providerId;
        this.dayOfWeek = dayOfWeek;
        this.specificDate = null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public AvailabilitySlot(UUID providerId, LocalDate specificDate, LocalTime startTime, LocalTime endTime) {
        this.providerId = providerId;
        this.dayOfWeek = null;
        this.specificDate = specificDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
