package ma.noba.user.domain.port;

import ma.noba.user.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    Optional<UserProfile> findByUserId(UUID userId);

    UserProfile save(UserProfile profile);

    void deleteByUserId(UUID userId);
}
