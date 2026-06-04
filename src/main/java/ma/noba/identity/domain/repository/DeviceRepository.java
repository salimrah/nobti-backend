package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByUserId(UUID userId);
    Optional<Device> findByUserIdAndFcmToken(UUID userId, String fcmToken);
    Optional<Device> findByFcmToken(String fcmToken);
    void deleteByUserIdAndFcmToken(UUID userId, String fcmToken);
}
