package ma.noba.reminder.infra;

import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import ma.noba.shared.domain.exception.PushDeliveryException;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import ma.noba.shared.domain.port.PushGateway;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "reminder.notification.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderNotificationJob.class);

    private final ServiceReminderRepository reminderRepository;
    private final FcmTokenQueryPort fcmTokenQueryPort;
    private final PushGateway pushGateway;

    public ReminderNotificationJob(ServiceReminderRepository reminderRepository,
                                   FcmTokenQueryPort fcmTokenQueryPort,
                                   PushGateway pushGateway) {
        this.reminderRepository = reminderRepository;
        this.fcmTokenQueryPort = fcmTokenQueryPort;
        this.pushGateway = pushGateway;
    }

    @Transactional
    @Scheduled(fixedRateString = "${reminder.notification.poll-interval-ms:900000}")
    @SchedulerLock(name = "reminder-notification-sweep", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void processDueReminders() {
        List<ServiceReminder> due = reminderRepository.findByActiveTrueAndNextReminderAtBefore(Instant.now());
        if (due.isEmpty()) return;

        log.info("Processing {} due service reminders", due.size());

        for (ServiceReminder reminder : due) {
            try {
                sendNotification(reminder);
                reminder.snooze();
                reminderRepository.save(reminder);
                log.debug("Snoozed reminder {} until {}", reminder.getId(), reminder.getNextReminderAt());
            } catch (Exception e) {
                log.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    private void sendNotification(ServiceReminder reminder) {
        String token = fcmTokenQueryPort.findFcmTokenByUserId(reminder.getUserId()).orElse(null);
        if (token == null) {
            log.warn("No push token for user {}, marking reminder inactive", reminder.getUserId());
            reminder.setActive(false);
            reminderRepository.save(reminder);
            return;
        }

        String body = "It's time for your " + reminder.getServiceName()
                + " at " + reminder.getProviderName() + "!";
        try {
            pushGateway.send(token, "Service Reminder", body, Map.of(
                    "type", "service_reminder",
                    "reminderId", reminder.getId().toString(),
                    "providerId", reminder.getProviderId().toString(),
                    "serviceName", reminder.getServiceName()));
            log.info("Service reminder notification sent to user {}", reminder.getUserId());
        } catch (PushDeliveryException e) {
            log.error("Failed to send reminder notification to user {}", reminder.getUserId(), e);
            if (e.isInvalidToken()) {
                log.info("Removing invalid push token for user {}", reminder.getUserId());
                fcmTokenQueryPort.invalidateToken(token);
            }
        }
    }
}
