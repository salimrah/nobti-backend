package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationSetting;

import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository {

    Optional<NotificationSetting> findByUserId(UUID userId);

    NotificationSetting saveSetting(NotificationSetting setting);
}
