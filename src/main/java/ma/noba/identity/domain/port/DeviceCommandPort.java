package ma.noba.identity.domain.port;

import java.util.UUID;

public interface DeviceCommandPort {
    void registerDevice(UUID userId, String fcmToken, String platform);
    void unregisterDevice(UUID userId, String fcmToken);
}
