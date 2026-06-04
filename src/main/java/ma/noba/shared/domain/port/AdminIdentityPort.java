package ma.noba.shared.domain.port;

import java.util.UUID;

public interface AdminIdentityPort {
    long countUsers();
    long countActiveUsers();
    long countSuspendedUsers();
    void suspendUser(UUID userId);
    void activateUser(UUID userId);
}
