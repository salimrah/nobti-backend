package ma.noba.notification.domain.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Public contract for feature modules. They request a notification; the notification module owns
 * persistence, preferences, fan-out, retries and the inbox.
 */
public interface NotificationPort {

    UUID send(Command command);

    enum Channel {
        IN_APP, PUSH, EMAIL, SMS, WHATSAPP
    }

    enum Category {
        TRANSACTIONAL, MARKETING, SYSTEM
    }

    record Command(
            UUID userId,
            String type,
            Category category,
            int priority,
            String title,
            String body,
            Map<String, Object> data,
            String dedupeKey,
            String locale,
            Set<Channel> channels,
            String email,
            String phone
    ) {
        public Command {
            channels = (channels == null || channels.isEmpty())
                    ? Set.of(Channel.IN_APP)
                    : Set.copyOf(channels);
        }
    }
}
