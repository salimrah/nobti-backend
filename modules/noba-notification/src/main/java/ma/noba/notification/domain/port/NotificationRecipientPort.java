package ma.noba.notification.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a user's contact details at send time, so the notification module never stores raw
 * email/phone numbers. Implemented in the composition root.
 */
public interface NotificationRecipientPort {

    Optional<String> email(UUID userId);

    Optional<String> phone(UUID userId);
}
