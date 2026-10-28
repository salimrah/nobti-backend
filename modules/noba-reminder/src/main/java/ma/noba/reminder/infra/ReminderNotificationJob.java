package ma.noba.reminder.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
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
import java.util.Set;

@Component
@ConditionalOnProperty(name = "reminder.notification.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReminderNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderNotificationJob.class);

    private final ServiceReminderRepository reminderRepository;
    private final NotificationPort notifications;

    @Transactional
    @Scheduled(fixedRateString = "${reminder.notification.poll-interval-ms:900000}")
    @SchedulerLock(name = "reminder-notification-sweep", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void processDueReminders() {
        List<ServiceReminder> due = reminderRepository.findByActiveTrueAndNextReminderAtBefore(Instant.now());
        if (due.isEmpty()) return;

        log.info("Processing {} due service reminders", due.size());

        for (ServiceReminder reminder : due) {
            try {
                notifications.send(new NotificationPort.Command(
                        reminder.getUserId(),
                        "reminder.service",
                        NotificationPort.Category.TRANSACTIONAL,
                        1,
                        "Service Reminder",
                        "It's time for your " + reminder.getServiceName()
                                + " at " + reminder.getProviderName() + "!",
                        Map.of(
                                "type", "service_reminder",
                                "reminderId", reminder.getId().toString(),
                                "providerId", reminder.getProviderId().toString(),
                                "serviceName", reminder.getServiceName()),
                        "reminder:" + reminder.getId() + ":" + reminder.getNextReminderAt(),
                        null,
                        Set.of(NotificationPort.Channel.PUSH),
                        null,
                        null));
                reminder.snooze();
                reminderRepository.save(reminder);
                log.debug("Snoozed reminder {} until {}", reminder.getId(), reminder.getNextReminderAt());
            } catch (Exception e) {
                log.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage());
            }
        }
    }
}
