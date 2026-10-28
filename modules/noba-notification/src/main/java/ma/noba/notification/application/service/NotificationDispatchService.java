package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationChannelPort;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.notification.domain.port.NotificationRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Polls due deliveries, sends them through the channel adapters, records the outcome and schedules
 * retries with backoff. Cross-instance safe via ShedLock.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notifications;
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEventRepository events;
    private final List<NotificationChannelPort> channels;

    @Scheduled(fixedDelayString = "${notification.dispatch.poll-interval-ms:5000}")
    @SchedulerLock(name = "notification-dispatch", lockAtMostFor = "2m", lockAtLeastFor = "1s")
    @Transactional
    public void dispatchDue() {
        List<NotificationDelivery> due = deliveries.findDue(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (due.isEmpty()) {
            return;
        }
        log.debug("Dispatching {} notifications", due.size());

        for (NotificationDelivery delivery : due) {
            Notification notification = notifications.findById(delivery.getNotificationId()).orElse(null);
            if (notification == null) {
                delivery.markSuppressed("NOTIFICATION_MISSING", "notification not found");
                deliveries.saveDelivery(delivery);
                continue;
            }

            NotificationChannelPort adapter = channels.stream()
                    .filter(c -> c.channel() == delivery.getChannel())
                    .findFirst()
                    .orElse(null);
            if (adapter == null) {
                delivery.markFailed("NO_ADAPTER", "no adapter for channel " + delivery.getChannel(), null);
                deliveries.saveDelivery(delivery);
                events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                        NotificationEventType.FAILED, Map.of("reason", "NO_ADAPTER")));
                refreshStatus(notification);
                continue;
            }

            delivery.markSending();
            long startedAt = System.nanoTime();
            try {
                adapter.send(notification, delivery);
                delivery.markSent(null, elapsedMs(startedAt));
                events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                        NotificationEventType.SENT, Map.of("channel", delivery.getChannel().name())));
            } catch (NotificationDeliveryException e) {
                handleFailure(delivery, e);
            } catch (Exception e) {
                handleFailure(delivery, new NotificationDeliveryException(false, true, e.getMessage(), e));
            }
            deliveries.saveDelivery(delivery);
            refreshStatus(notification);
        }
    }

    private void handleFailure(NotificationDelivery delivery, NotificationDeliveryException e) {
        if (e.isInvalidRecipient()) {
            delivery.markSuppressed("INVALID_RECIPIENT", e.getMessage());
            events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                    NotificationEventType.SUPPRESSED, Map.of("reason", "INVALID_RECIPIENT")));
            return;
        }
        boolean retry = e.isRetryable() && delivery.getAttempt() < MAX_ATTEMPTS;
        Instant nextAttemptAt = retry ? Instant.now().plus(backoff(delivery.getAttempt())) : null;
        delivery.markFailed(retry ? "DELIVERY_FAILED" : "PERMANENT_FAILURE", e.getMessage(), nextAttemptAt);
        events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                NotificationEventType.FAILED, Map.of("retry", retry)));
    }

    private void refreshStatus(Notification notification) {
        List<NotificationDelivery> all = deliveries.findByNotificationId(notification.getId());
        boolean pending = all.stream().anyMatch(d -> !d.terminal());
        boolean success = all.stream().anyMatch(d -> d.getStatus() == ma.noba.notification.domain.model.DeliveryStatus.SENT
                || d.getStatus() == ma.noba.notification.domain.model.DeliveryStatus.DELIVERED);
        boolean failure = all.stream().anyMatch(d -> d.getStatus() == ma.noba.notification.domain.model.DeliveryStatus.FAILED
                || d.getStatus() == ma.noba.notification.domain.model.DeliveryStatus.SUPPRESSED);

        NotificationStatus status;
        if (pending) {
            status = NotificationStatus.QUEUED;
        } else if (failure && success) {
            status = NotificationStatus.PARTIAL;
        } else if (failure) {
            status = NotificationStatus.FAILED;
        } else {
            status = NotificationStatus.SENT;
        }
        if (notification.getStatus() != status) {
            notification.setStatus(status);
            notifications.saveNotification(notification);
        }
    }

    private static int elapsedMs(long startedAt) {
        return (int) ((System.nanoTime() - startedAt) / 1_000_000);
    }

    private static Duration backoff(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
    }
}
