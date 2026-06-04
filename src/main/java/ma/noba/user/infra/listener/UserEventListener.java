package ma.noba.user.infra.listener;

import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import ma.noba.user.domain.model.UserView;
import ma.noba.user.domain.port.UserViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final UserViewRepository userViewRepository;

    public UserEventListener(UserViewRepository userViewRepository) {
        this.userViewRepository = userViewRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onUserSignedUp(UserSignedUpEvent event) {
        log.info("onUserSignedUp event received: userId={}, phone={}, status={}", event.getUserId(), event.getPhone(), event.getStatus());
        UserView view = new UserView(
                UUID.fromString(event.getUserId()),
                event.getEmail(),
                event.getPhone(),
                event.getStatus(),
                event.getOnboardingStatus(),
                java.time.Instant.now()
        );
        var saved = userViewRepository.save(view);
        log.info("UserView saved successfully for userId={}, id={}", event.getUserId(), saved.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        userViewRepository.findById(event.getUserId()).ifPresent(view -> {
            view.setStatus(event.getNewStatus());
            userViewRepository.save(view);
        });
    }
}
