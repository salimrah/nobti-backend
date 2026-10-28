package ma.noba.shared.domain.port;

import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read model for the admin console: users with their profile and roles. */
public interface AdminUserDirectoryPort {

    PagedResponse<UserRow> searchUsers(String query, UserStatus status, String role, int page, int size);

    /** Display name per user id, for queue members and other admin views. */
    Map<UUID, String> displayNames(Collection<UUID> userIds);

    record UserRow(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            UserStatus status,
            Set<String> roles,
            Instant createdAt) {
    }
}
