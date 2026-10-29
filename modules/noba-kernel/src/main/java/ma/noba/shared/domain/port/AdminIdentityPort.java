package ma.noba.shared.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminIdentityPort {
    long countUsers();
    long countActiveUsers();
    long countSuspendedUsers();
    void suspendUser(UUID userId);
    void activateUser(UUID userId);

    Optional<UserState> findUser(UUID userId);

    List<SessionRow> sessions(UUID userId);

    void revokeSessions(UUID userId);

    /**
     * Provisions a verified, active account with a password (used to create operations staff).
     *
     * @return the new user id
     */
    UUID createAdmin(String email, String rawPassword, String firstName, String lastName);

    record SessionRow(UUID id, Instant createdAt, Instant expiresAt, Instant lastUsedAt) {
    }

    record UserState(UUID id, String status, String email, String phone, Instant createdAt) {
    }
}
