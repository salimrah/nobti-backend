package ma.noba.identity.domain.port;

import java.util.UUID;

public interface UserProfileProvisioningPort {
    void createProfile(UUID userId, String firstName, String lastName, String displayName);
}
