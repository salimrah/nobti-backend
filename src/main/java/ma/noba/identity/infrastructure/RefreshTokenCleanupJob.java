package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.repository.RefreshTokenRepository;
import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshRepo;
    private final JwtTokenProperties props;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshRepo, JwtTokenProperties props) {
        this.refreshRepo = refreshRepo;
        this.props = props;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "refresh-token-cleanup-expired", lockAtMostFor = "1h", lockAtLeastFor = "1m")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now();
        int deleted = refreshRepo.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }

    @Scheduled(cron = "0 30 4 * * *")
    @SchedulerLock(name = "refresh-token-cleanup-idle", lockAtMostFor = "1h", lockAtLeastFor = "1m")
    @Transactional
    public void cleanupIdleTokens() {
        int idleDays = props.jwt.idleTimeoutDays;
        Instant cutoff = Instant.now().minusSeconds(idleDays * 86400L);
        int revoked = refreshRepo.revokeIdleTokens(cutoff);
        if (revoked > 0) {
            log.info("Revoked {} idle refresh tokens (no activity for {} days)", revoked, idleDays);
        }
    }
}
