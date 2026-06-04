package ma.noba.identity.infrastructure.adapter;

import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminIdentityPortAdapter implements AdminIdentityPort {

    private final UserRepository userRepository;
    private final EventBus eventBus;

    public AdminIdentityPortAdapter(UserRepository userRepository, EventBus eventBus) {
        this.userRepository = userRepository;
        this.eventBus = eventBus;
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    @Override
    public long countSuspendedUsers() {
        return userRepository.countByStatus(UserStatus.DISABLED);
    }

    @Override
    public void suspendUser(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            var oldStatus = user.getStatus();
            user.setStatus(UserStatus.DISABLED);
            userRepository.save(user);
            eventBus.publish(new UserStatusChangedEvent(userId, oldStatus, UserStatus.DISABLED));
        });
    }

    @Override
    public void activateUser(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            var oldStatus = user.getStatus();
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            eventBus.publish(new UserStatusChangedEvent(userId, oldStatus, UserStatus.ACTIVE));
        });
    }
}
