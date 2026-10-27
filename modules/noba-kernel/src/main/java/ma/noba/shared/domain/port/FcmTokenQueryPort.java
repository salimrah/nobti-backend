package ma.noba.shared.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface FcmTokenQueryPort {
    Optional<String> findFcmTokenByUserId(UUID userId);
    void invalidateToken(String fcmToken);
}
