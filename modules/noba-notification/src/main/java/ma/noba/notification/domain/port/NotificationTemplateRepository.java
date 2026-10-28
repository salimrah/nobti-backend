package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository {

    /** Highest active version for the exact key + locale. */
    Optional<NotificationTemplate> findActive(String templateKey, String locale);

    List<NotificationTemplate> findAllVersions();

    Optional<NotificationTemplate> findTemplateById(UUID id);

    int nextVersion(String templateKey, String locale);

    /** Keeps a single active version per key + locale. */
    int deactivateOthers(String templateKey, String locale, UUID keepId);

    NotificationTemplate saveTemplate(NotificationTemplate template);
}
