package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<NotificationTemplate> findAllByOrderByTemplateKeyAscLocaleAscVersionDesc();

    @Override
    default List<NotificationTemplate> findAllVersions() {
        return findAllByOrderByTemplateKeyAscLocaleAscVersionDesc();
    }

    @Override
    default Optional<NotificationTemplate> findTemplateById(UUID id) {
        return findById(id);
    }

    @Query("select coalesce(max(t.version), 0) from NotificationTemplate t " +
            "where t.templateKey = :key and t.locale = :locale")
    Integer maxVersion(@Param("key") String templateKey, @Param("locale") String locale);

    @Override
    default int nextVersion(String templateKey, String locale) {
        Integer current = maxVersion(templateKey, locale);
        return (current == null ? 0 : current) + 1;
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationTemplate t set t.active = false " +
            "where t.templateKey = :key and t.locale = :locale and t.id <> :keepId")
    int deactivateSiblings(@Param("key") String templateKey, @Param("locale") String locale,
                           @Param("keepId") UUID keepId);

    @Override
    default int deactivateOthers(String templateKey, String locale, UUID keepId) {
        return deactivateSiblings(templateKey, locale, keepId);
    }

    @Override
    default NotificationTemplate saveTemplate(NotificationTemplate template) {
        return save(template);
    }
}
