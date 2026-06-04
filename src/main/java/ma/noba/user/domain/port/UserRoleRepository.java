package ma.noba.user.domain.port;

import ma.noba.user.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository {

    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByRoleName(String roleName);
}
