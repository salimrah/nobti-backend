package ma.noba.identity.domain.service;

import java.util.Set;
import java.util.UUID;

public interface UserRolePort {
    Set<String> findRolesByUserId(UUID userId);
    void assignRole(UUID userId, String role);
}
