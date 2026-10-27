package ma.noba.badge.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_badge", uniqueConstraints = @UniqueConstraint(columnNames = {"providerId", "badgeType"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderBadge {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType badgeType;

    @Column(nullable = false)
    private Instant awardedAt;

    private Instant expiresAt;

    @Column(nullable = false)
    private boolean isAutoAwarded;

    public ProviderBadge(UUID providerId, BadgeType badgeType, boolean isAutoAwarded) {
        this.providerId = providerId;
        this.badgeType = badgeType;
        this.isAutoAwarded = isAutoAwarded;
        this.awardedAt = Instant.now();
    }

    public ProviderBadge(UUID providerId, BadgeType badgeType, boolean isAutoAwarded, Instant expiresAt) {
        this(providerId, badgeType, isAutoAwarded);
        this.expiresAt = expiresAt;
    }
}
