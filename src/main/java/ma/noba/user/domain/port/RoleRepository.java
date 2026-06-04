package ma.noba.user.domain.port;

import ma.noba.user.domain.model.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Optional<Role> findById(UUID id);
    Role findByName(String name);
}
