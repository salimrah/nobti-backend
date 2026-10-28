package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.notification.domain.port.NotificationRecipientPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRecipientAdapter implements NotificationRecipientPort {

    private final UserRepository users;

    @Override
    public Optional<String> email(UUID userId) {
        return users.findById(userId)
                .map(User::getEmail)
                .filter(value -> value != null && !value.isBlank());
    }

    @Override
    public Optional<String> phone(UUID userId) {
        return users.findById(userId)
                .map(User::getPhone)
                .filter(value -> value != null && !value.isBlank());
    }
}
