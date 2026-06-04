package ma.noba.user.infra;

import ma.noba.user.domain.model.NotificationPreference;
import ma.noba.user.domain.port.NotificationPreferenceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID>, NotificationPreferenceRepository {

    Optional<NotificationPreference> findByUserId(UUID userId);
}