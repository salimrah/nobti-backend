package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationSetting;
import ma.noba.notification.domain.port.NotificationSettingRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationSettingRepository
        extends JpaRepository<NotificationSetting, UUID>, NotificationSettingRepository {

    @Override
    Optional<NotificationSetting> findByUserId(UUID userId);

    @Override
    default NotificationSetting saveSetting(NotificationSetting setting) {
        return save(setting);
    }
}
