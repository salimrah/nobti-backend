package ma.noba.notification.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.service.NotificationDigestService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs hourly and flushes the digests that fall inside their user's local digest hour. ShedLock
 * keeps a single instance in charge in a multi-node deployment.
 */
@Component
@ConditionalOnProperty(name = "notification.digest.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationDigestJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationDigestJob.class);

    private final NotificationDigestService digests;

    @Scheduled(cron = "${notification.digest.cron:0 5 * * * *}")
    @SchedulerLock(name = "notification-digest", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void run() {
        int processed = digests.flushDue();
        if (processed > 0) {
            log.info("Flushed {} notification digests", processed);
        }
    }
}
