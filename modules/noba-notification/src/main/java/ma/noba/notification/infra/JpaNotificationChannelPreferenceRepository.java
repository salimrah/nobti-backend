package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationChannelPreference;
import ma.noba.notification.domain.port.NotificationChannelPreferenceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationChannelPreferenceRepository
        extends JpaRepository<NotificationChannelPreference, UUID>, NotificationChannelPreferenceRepository {

    @Override
    List<NotificationChannelPreference> findByUserId(UUID userId);

    Optional<NotificationChannelPreference> findByUserIdAndCategoryAndChannel(
            UUID userId, NotificationCategory category, NotificationChannel channel);

    @Override
    default Optional<NotificationChannelPreference> find(UUID userId, NotificationCategory category,
                                                         NotificationChannel channel) {
        return findByUserIdAndCategoryAndChannel(userId, category, channel);
    }

    @Override
    default NotificationChannelPreference savePreference(NotificationChannelPreference preference) {
        return save(preference);
    }
}
