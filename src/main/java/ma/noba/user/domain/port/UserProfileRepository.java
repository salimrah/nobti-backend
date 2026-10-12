package ma.noba.user.domain.port;

import ma.noba.user.domain.model.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    Optional<UserProfile> findByUserId(UUID userId);

    List<UserProfile> findByUserIdIn(Collection<UUID> userIds);

    UserProfile save(UserProfile profile);

    void deleteByUserId(UUID userId);
}
