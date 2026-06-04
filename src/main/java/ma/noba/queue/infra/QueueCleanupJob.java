package ma.noba.queue.infra;

import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueueStatus;
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.shared.infra.lock.RedisDistributedLock;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class QueueCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(QueueCleanupJob.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final QueueRepository queueRepository;
    private final QueueMemberRepository memberRepository;
    private final RedisDistributedLock distributedLock;

    public QueueCleanupJob(QueueRepository queueRepository, QueueMemberRepository memberRepository,
                           RedisDistributedLock distributedLock) {
        this.queueRepository = queueRepository;
        this.memberRepository = memberRepository;
        this.distributedLock = distributedLock;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupStaleQueues() {
        if (!distributedLock.tryLock("queue-cleanup-stale", LOCK_TTL)) {
            log.info("Skipping stale queue cleanup — another instance holds the lock");
            return;
        }

        try {
            log.info("Starting queue cleanup job");

            List<Queue> allQueues = queueRepository.findAll();
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            int cleaned = 0;

            for (Queue queue : allQueues) {
                if (queue.getStatus() == QueueStatus.CLOSED && queue.getUpdatedAt().isBefore(cutoff)) {
                    queueRepository.delete(queue);
                    cleaned++;
                }
            }

            log.info("Queue cleanup completed. Removed {} stale queues", cleaned);
        } finally {
            distributedLock.release("queue-cleanup-stale");
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cancelAbandonedMembers() {
        if (!distributedLock.tryLock("queue-cleanup-abandoned", LOCK_TTL)) {
            log.info("Skipping abandoned member cleanup — another instance holds the lock");
            return;
        }

        try {
            log.info("Starting abandoned member cleanup");

            Instant cutoff = Instant.now().minus(2, ChronoUnit.HOURS);
            var activeStatuses = List.of(QueueMemberStatus.WAITING, QueueMemberStatus.NOTIFIED, QueueMemberStatus.ACTIVE);
            var abandoned = memberRepository.findByStatusInAndCreatedAtBefore(activeStatuses, cutoff);
            int cancelled = 0;

            for (var member : abandoned) {
                member.cancel();
                memberRepository.save(member);
                cancelled++;
            }

            log.info("Abandoned member cleanup completed. Cancelled {} members", cancelled);
        } finally {
            distributedLock.release("queue-cleanup-abandoned");
        }
    }
}
