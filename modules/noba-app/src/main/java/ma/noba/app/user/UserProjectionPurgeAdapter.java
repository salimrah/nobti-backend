package ma.noba.app.user;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.port.UserProjectionPurgePort;
import ma.noba.user.domain.port.UserViewRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserProjectionPurgeAdapter implements UserProjectionPurgePort {

    private final UserViewRepository userViews;

    @Override
    public void purge(UUID userId) {
        userViews.deleteById(userId);
    }
}
