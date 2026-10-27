package ma.noba.user.domain.port;

import ma.noba.user.domain.model.NotificationPreference;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {

    Optional<NotificationPreference> findByUserId(UUID userId);

    NotificationPreference save(NotificationPreference preference);
}