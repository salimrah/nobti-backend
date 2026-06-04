package ma.noba.queue.infra;

import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.shared.infra.lock.RedisDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class ReminderSafetyNet {

    private static final Logger log = LoggerFactory.getLogger(ReminderSafetyNet.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(2);

    private final QueueMemberRepository memberRepository;
    private final QueueNotificationPort notificationPort;
    private final RedisDistributedLock distributedLock;

    @Value("${queue.notification.reminder-before-minutes:5}")
    private int reminderBeforeMinutes;

    public ReminderSafetyNet(QueueMemberRepository memberRepository, QueueNotificationPort notificationPort,
                             RedisDistributedLock distributedLock) {
        this.memberRepository = memberRepository;
        this.notificationPort = notificationPort;
        this.distributedLock = distributedLock;
    }

    @Scheduled(fixedRateString = "${queue.notification.poll-interval-ms:60000}")
    public void checkPendingReminders() {
        if (!distributedLock.tryLock("reminder-safety-net", LOCK_TTL)) {
            return;
        }

        try {
            Instant now = Instant.now();
            Instant threshold = now.plus(reminderBeforeMinutes, ChronoUnit.MINUTES);

            List<QueueMember> due = memberRepository.findDueForReminder(
                    QueueMemberStatus.WAITING,
                    threshold
            );

            if (due.isEmpty()) {
                return;
            }

            log.info("Safety net: found {} members approaching their turn", due.size());

            for (QueueMember member : due) {
                try {
                    member.notifyClient();
                    memberRepository.save(member);
                    notificationPort.notifyEstimatedWait(member.getClientId(), member.getQueue().getProviderId(), member.getPosition());
                    log.info("Safety net: sent reminder to client {} (position {}, estimated start {})",
                            member.getClientId(), member.getPosition(), member.getEstimatedStartTime());
                } catch (Exception e) {
                    log.error("Safety net: failed to notify client {}", member.getClientId(), e);
                }
            }
        } finally {
            distributedLock.release("reminder-safety-net");
        }
    }
}
