package ma.noba.shared.domain.user;

import java.util.Set;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        String phone,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isProvider() {
        return hasRole("PROVIDER");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
