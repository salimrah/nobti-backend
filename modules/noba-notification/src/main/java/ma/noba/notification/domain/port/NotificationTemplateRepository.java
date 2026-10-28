package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationTemplate;

import java.util.Optional;

public interface NotificationTemplateRepository {

    /** Highest active version for the exact key + locale. */
    Optional<NotificationTemplate> findActive(String templateKey, String locale);
}
