package ma.noba.provider.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "business_hours", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_id", "day_of_week"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BusinessHours {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private int dayOfWeek;

    private LocalTime opensAt;

    private LocalTime closesAt;

    @Column(nullable = false)
    private boolean closed;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public BusinessHours(UUID providerId, int dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closed) {
        this.providerId = providerId;
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.closed = closed;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static BusinessHours closedDay(UUID providerId, int dayOfWeek) {
        return new BusinessHours(providerId, dayOfWeek, null, null, true);
    }

    public static BusinessHours workingDay(UUID providerId, int dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        return new BusinessHours(providerId, dayOfWeek, opensAt, closesAt, false);
    }

    public boolean isOpenNow(LocalTime now) {
        if (closed) return false;
        return !now.isBefore(opensAt) && !now.isAfter(closesAt);
    }
}
