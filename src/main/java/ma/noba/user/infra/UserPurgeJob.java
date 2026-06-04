package ma.noba.user.infra;

import ma.noba.shared.domain.model.UserStatus;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.user.domain.port.UserViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class UserPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(UserPurgeJob.class);
    static final int PURGE_AFTER_DAYS = 30;

    private final UserRepository userRepository;
    private final UserViewRepository userViewRepository;

    public UserPurgeJob(UserRepository userRepository, UserViewRepository userViewRepository) {
        this.userRepository = userRepository;
        this.userViewRepository = userViewRepository;
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void purgeDeletedUsers() {
        Instant cutoff = Instant.now().minusSeconds(PURGE_AFTER_DAYS * 86400L);
        var candidates = userRepository.findByStatusAndDeletedAtBefore(UserStatus.DELETED, cutoff);

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Purging {} users deleted before {}", candidates.size(), cutoff);
        for (var user : candidates) {
            try {
                userViewRepository.deleteById(user.getId());
                userRepository.delete(user);
                log.debug("Purged user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to purge user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("User purge completed");
    }
}
