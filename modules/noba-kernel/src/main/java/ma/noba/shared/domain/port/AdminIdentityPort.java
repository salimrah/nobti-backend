package ma.noba.shared.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface AdminIdentityPort {
    long countUsers();
    long countActiveUsers();
    long countSuspendedUsers();
    void suspendUser(UUID userId);
    void activateUser(UUID userId);

    Optional<UserState> findUser(UUID userId);

    record UserState(UUID id, String status) {
    }
}
