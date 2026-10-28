package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.user.domain.model.NotificationPreference;
import ma.noba.user.domain.port.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Bridges the notification module's preference port to the user module's stored preferences. */
@Component
@RequiredArgsConstructor
public class NotificationPreferenceAdapter implements NotificationPreferencePort {

    private final NotificationPreferenceRepository preferences;

    @Override
    public boolean isEnabled(UUID userId, NotificationChannel channel) {
        if (channel == NotificationChannel.IN_APP) {
            return true;
        }
        NotificationPreference preference = preferences.findByUserId(userId).orElse(null);
        if (preference == null) {
            return true; // default: all channels enabled
        }
        return switch (channel) {
            case PUSH -> preference.isPushEnabled();
            case EMAIL -> preference.isEmailEnabled();
            case SMS, WHATSAPP -> preference.isSmsEnabled();
            default -> true;
        };
    }
}
