package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.DigestFrequency;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationChannelPreference;
import ma.noba.notification.domain.port.NotificationChannelPreferenceRepository;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.notification.domain.port.NotificationSettingRepository;
import ma.noba.user.domain.model.NotificationPreference;
import ma.noba.user.domain.port.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the per-category matrix, falling back to the legacy three booleans for users that
 * predate the migration.
 */
@Component
@RequiredArgsConstructor
public class NotificationPreferenceAdapter implements NotificationPreferencePort {

    private final NotificationChannelPreferenceRepository channelPreferences;
    private final NotificationSettingRepository settings;
    private final NotificationPreferenceRepository legacyPreferences;

    @Override
    public boolean isEnabled(UUID userId, NotificationCategory category, NotificationChannel channel) {
        if (channel == NotificationChannel.IN_APP) {
            return true;
        }
        Optional<NotificationChannelPreference> preference =
                channelPreferences.find(userId, category, channel);
        if (preference.isPresent()) {
            return preference.get().isEnabled();
        }
        return legacyEnabled(userId, channel);
    }

    @Override
    public Optional<QuietHours> quietHours(UUID userId) {
        return settings.findByUserId(userId)
                .filter(s -> s.getQuietHoursStart() != null
                        && s.getQuietHoursEnd() != null
                        && s.getTimezone() != null
                        && !s.getTimezone().isBlank())
                .map(s -> new QuietHours(s.getQuietHoursStart(), s.getQuietHoursEnd(), s.getTimezone()));
    }

    @Override
    public Optional<Digest> digest(UUID userId) {
        return settings.findByUserId(userId)
                .map(s -> new Digest(DigestFrequency.parse(s.getDigest()), s.getTimezone()));
    }

    private boolean legacyEnabled(UUID userId, NotificationChannel channel) {
        NotificationPreference preference = legacyPreferences.findByUserId(userId).orElse(null);
        if (preference == null) {
            return true;
        }
        return switch (channel) {
            case PUSH -> preference.isPushEnabled();
            case EMAIL -> preference.isEmailEnabled();
            case SMS, WHATSAPP -> preference.isSmsEnabled();
            default -> true;
        };
    }
}
