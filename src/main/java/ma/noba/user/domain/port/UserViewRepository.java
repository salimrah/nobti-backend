package ma.noba.user.domain.port;

import ma.noba.user.domain.model.UserView;

import java.util.Optional;
import java.util.UUID;

public interface UserViewRepository {
    Optional<UserView> findById(UUID userId);
    UserView save(UserView userView);
    void deleteById(UUID userId);
}
