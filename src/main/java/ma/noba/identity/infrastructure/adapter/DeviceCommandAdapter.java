package ma.noba.identity.infrastructure.adapter;

import ma.noba.identity.domain.model.Device;
import ma.noba.identity.domain.port.DeviceCommandPort;
import ma.noba.identity.domain.repository.DeviceRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DeviceCommandAdapter implements DeviceCommandPort {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceCommandAdapter(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void registerDevice(UUID userId, String fcmToken, String platform) {
        deviceRepository.deleteByUserIdAndFcmToken(userId, fcmToken);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        var device = Device.builder()
                .user(user)
                .fcmToken(fcmToken)
                .platform(platform)
                .lastSeenAt(Instant.now())
                .build();
        deviceRepository.save(device);
    }

    @Override
    public void unregisterDevice(UUID userId, String fcmToken) {
        deviceRepository.deleteByUserIdAndFcmToken(userId, fcmToken);
    }
}
