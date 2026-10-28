package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationTemplateRepository
        extends JpaRepository<NotificationTemplate, UUID>, NotificationTemplateRepository {

    Optional<NotificationTemplate> findFirstByTemplateKeyAndLocaleAndActiveTrueOrderByVersionDesc(
            String templateKey, String locale);

    @Override
    default Optional<NotificationTemplate> findActive(String templateKey, String locale) {
        return findFirstByTemplateKeyAndLocaleAndActiveTrueOrderByVersionDesc(templateKey, locale);
    }
}
