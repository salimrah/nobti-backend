package ma.noba.shared.domain.port;

import java.util.Set;
import java.util.UUID;

/** Role membership read/write for the admin console. Owned by the user module. */
public interface AdminRolePort {

    Set<String> rolesOf(UUID userId);

    void replaceRoles(UUID userId, Set<String> roles);

    long countAdmins();

    /** Roles an administrator may assign. */
    Set<String> assignableRoles();
}
