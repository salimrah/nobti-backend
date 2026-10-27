package ma.noba.identity.infra.adapter;

import ma.noba.identity.domain.repository.DeviceRepository;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FcmTokenQueryPortAdapter implements FcmTokenQueryPort {

    private final DeviceRepository deviceRepository;

    public FcmTokenQueryPortAdapter(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Optional<String> findFcmTokenByUserId(UUID userId) {
        return deviceRepository.findByUserId(userId).stream()
                .findFirst()
                .map(d -> d.getFcmToken());
    }

    @Override
    public void invalidateToken(String fcmToken) {
        deviceRepository.findByFcmToken(fcmToken)
                .ifPresent(d -> deviceRepository.deleteByUserIdAndFcmToken(d.getUser().getId(), fcmToken));
    }
}
