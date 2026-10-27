package ma.noba.provider.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-provider configuration. A missing row means "use defaults" — the application layer never
 * requires a row to exist, so provider onboarding does not have to create one up front.
 */
@Entity
@Table(name = "provider_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_provider_settings_provider", columnNames = "provider_id"))
@Getter
@Setter
@NoArgsConstructor
public class ProviderSettings {

    public static final int DEFAULT_MAX_PARTY_SIZE = 10;
    public static final int DEFAULT_NO_SHOW_GRACE_MINUTES = 10;
    public static final int DEFAULT_CANCELLATION_WINDOW_MINUTES = 60;
    public static final int DEFAULT_LOYALTY_POINTS_PER_VISIT = 10;
    public static final String DEFAULT_LOCALE = "en";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private UUID providerId;

    @Column(name = "max_party_size", nullable = false)
    private int maxPartySize = DEFAULT_MAX_PARTY_SIZE;

    @Column(name = "walk_ins_enabled", nullable = false)
    private boolean walkInsEnabled = true;

    @Column(name = "appointments_enabled", nullable = false)
    private boolean appointmentsEnabled = true;

    @Column(name = "no_show_grace_minutes", nullable = false)
    private int noShowGraceMinutes = DEFAULT_NO_SHOW_GRACE_MINUTES;

    @Column(name = "cancellation_window_minutes", nullable = false)
    private int cancellationWindowMinutes = DEFAULT_CANCELLATION_WINDOW_MINUTES;

    @Column(name = "auto_fill_enabled", nullable = false)
    private boolean autoFillEnabled = true;

    @Column(name = "loyalty_points_per_visit", nullable = false)
    private int loyaltyPointsPerVisit = DEFAULT_LOYALTY_POINTS_PER_VISIT;

    @Column(name = "notification_locale", nullable = false, length = 8)
    private String notificationLocale = DEFAULT_LOCALE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static ProviderSettings defaults(UUID providerId) {
        ProviderSettings settings = new ProviderSettings();
        settings.providerId = providerId;
        return settings;
    }

    public void apply(int maxPartySize,
                      boolean walkInsEnabled,
                      boolean appointmentsEnabled,
                      int noShowGraceMinutes,
                      int cancellationWindowMinutes,
                      boolean autoFillEnabled,
                      int loyaltyPointsPerVisit,
                      String notificationLocale) {
        this.maxPartySize = maxPartySize;
        this.walkInsEnabled = walkInsEnabled;
        this.appointmentsEnabled = appointmentsEnabled;
        this.noShowGraceMinutes = noShowGraceMinutes;
        this.cancellationWindowMinutes = cancellationWindowMinutes;
        this.autoFillEnabled = autoFillEnabled;
        this.loyaltyPointsPerVisit = loyaltyPointsPerVisit;
        this.notificationLocale = notificationLocale;
    }
}
