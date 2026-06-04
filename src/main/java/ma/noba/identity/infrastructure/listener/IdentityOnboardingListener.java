package ma.noba.identity.infrastructure.listener;

import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.event.UserOnboardingCompletedEvent;
import ma.noba.shared.domain.model.OnboardingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IdentityOnboardingListener {

    private static final Logger log = LoggerFactory.getLogger(IdentityOnboardingListener.class);

    private final UserRepository userRepository;

    public IdentityOnboardingListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onUserOnboardingCompleted(UserOnboardingCompletedEvent event) {
        userRepository.findById(event.getUserId()).ifPresentOrElse(user -> {
            user.setOnboardingStatus(OnboardingStatus.COMPLETED);
            userRepository.save(user);
            log.info("Onboarding completed for userId={}", event.getUserId());
        }, () -> log.warn("User not found for onboarding completion: userId={}", event.getUserId()));
    }
}
