package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationChannelPreference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationChannelPreferenceRepository {

    List<NotificationChannelPreference> findByUserId(UUID userId);

    Optional<NotificationChannelPreference> find(UUID userId, NotificationCategory category,
                                                 NotificationChannel channel);

    NotificationChannelPreference savePreference(NotificationChannelPreference preference);
}
