package ma.noba.identity.infrastructure.listener;

import ma.noba.identity.domain.model.User;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.event.AccountDeletionRequested;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class AccountDeletionListener {

    private final UserRepository userRepository;
    private final EventBus eventBus;

    public AccountDeletionListener(UserRepository userRepository, EventBus eventBus) {
        this.userRepository = userRepository;
        this.eventBus = eventBus;
    }

    @TransactionalEventListener
    public void onAccountDeletionRequested(AccountDeletionRequested event) {
        userRepository.findById(event.getUserId()).ifPresent(user -> {
            UserStatus oldStatus = user.getStatus();
            user.setStatus(UserStatus.DELETED);
            user.setDeletedAt(Instant.now());
            userRepository.save(user);
            eventBus.publish(new UserStatusChangedEvent(event.getUserId(), oldStatus, UserStatus.DELETED));
        });
    }
}
