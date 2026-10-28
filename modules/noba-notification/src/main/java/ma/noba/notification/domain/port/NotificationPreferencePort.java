package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/** Reads the user's channel preferences and quiet hours without depending on the user module. */
public interface NotificationPreferencePort {

    boolean isEnabled(UUID userId, NotificationCategory category, NotificationChannel channel);

    Optional<QuietHours> quietHours(UUID userId);

    record QuietHours(LocalTime start, LocalTime end, String timezone) {
    }
}
