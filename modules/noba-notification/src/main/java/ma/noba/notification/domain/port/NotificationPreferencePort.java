package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.DigestFrequency;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/** Reads the user's channel preferences, quiet hours and digest frequency. */
public interface NotificationPreferencePort {

    boolean isEnabled(UUID userId, NotificationCategory category, NotificationChannel channel);

    Optional<QuietHours> quietHours(UUID userId);

    /** Empty when the user never configured anything (treated as {@link DigestFrequency#NONE}). */
    Optional<Digest> digest(UUID userId);

    record QuietHours(LocalTime start, LocalTime end, String timezone) {
    }

    record Digest(DigestFrequency frequency, String timezone) {
        public boolean enabled() {
            return frequency != null && frequency != DigestFrequency.NONE;
        }
    }
}
