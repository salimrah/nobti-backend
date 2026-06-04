package ma.noba.queue.infra.listener;

import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.event.ClientTurnStartedEvent;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.infra.QueueScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class QueueEventListener {

    private static final Logger log = LoggerFactory.getLogger(QueueEventListener.class);

    private final QueueNotificationPort notificationPort;
    private final QueueScheduler queueScheduler;

    @Value("${queue.notification.reminder-before-minutes:5}")
    private int reminderBeforeMinutes;

    public QueueEventListener(QueueNotificationPort notificationPort, QueueScheduler queueScheduler) {
        this.notificationPort = notificationPort;
        this.queueScheduler = queueScheduler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientJoined(ClientJoinedQueueEvent event) {
        log.info("Client {} joined queue for provider {} at position {}, estimated start: {}",
                event.clientId(), event.providerId(), event.position(), event.estimatedStartTime());

        notificationPort.notifyEstimatedWait(event.clientId(), event.providerId(), event.position());

        Instant reminderTime = event.estimatedStartTime().minus(reminderBeforeMinutes, ChronoUnit.MINUTES);

        if (reminderTime.isAfter(Instant.now())) {
            queueScheduler.scheduleTurnReminder(event.clientId(), event.providerId(), event.position(), reminderTime);
            log.info("Scheduled turn reminder for client {} at {} ({} min before turn)",
                    event.clientId(), reminderTime, reminderBeforeMinutes);
        } else {
            log.info("Skipping reminder for client {} — estimated start is too soon ({} min away)",
                    event.clientId(), Duration.between(Instant.now(), event.estimatedStartTime()).toMinutes());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientLeft(ClientLeftQueueEvent event) {
        log.info("Client {} left queue for provider {}",
                event.clientId(), event.providerId());
        queueScheduler.cancelTurnReminder(event.clientId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTurnStarted(ClientTurnStartedEvent event) {
        log.info("Turn started for client {} at provider {}",
                event.clientId(), event.providerId());
        notificationPort.notifyTurnStart(event.clientId(), event.providerId(), 0);
        queueScheduler.cancelTurnReminder(event.clientId());
    }
}
