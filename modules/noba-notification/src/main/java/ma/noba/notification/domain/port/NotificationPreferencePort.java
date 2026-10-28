package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationChannel;

import java.util.UUID;

/** Reads the user's channel preferences without depending on the user module. */
public interface NotificationPreferencePort {

    boolean isEnabled(UUID userId, NotificationChannel channel);
}
