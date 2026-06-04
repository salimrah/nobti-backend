package ma.noba.reminder.infra;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "reminder.notification.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderNotificationJob.class);

    private final ServiceReminderRepository reminderRepository;
    private final FcmTokenQueryPort fcmTokenQueryPort;
    private final FirebaseMessaging firebaseMessaging;

    public ReminderNotificationJob(ServiceReminderRepository reminderRepository,
                                   FcmTokenQueryPort fcmTokenQueryPort,
                                   FirebaseMessaging firebaseMessaging) {
        this.reminderRepository = reminderRepository;
        this.fcmTokenQueryPort = fcmTokenQueryPort;
        this.firebaseMessaging = firebaseMessaging;
    }

    @Transactional
    @Scheduled(fixedRateString = "${reminder.notification.poll-interval-ms:900000}")
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
            log.warn("No FCM token for user {}, marking reminder inactive", reminder.getUserId());
            reminder.setActive(false);
            reminderRepository.save(reminder);
            return;
        }

        try {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle("Service Reminder")
                    .setBody("It's time for your " + reminder.getServiceName() + " at " + reminder.getProviderName() + "!")
                    .build())
                .putData("type", "service_reminder")
                .putData("reminderId", reminder.getId().toString())
                .putData("providerId", reminder.getProviderId().toString())
                .putData("serviceName", reminder.getServiceName())
                .build();

            firebaseMessaging.send(message);
            log.info("Service reminder notification sent to user {}", reminder.getUserId());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send reminder notification to user {}", reminder.getUserId(), e);
            handleError(token, e);
        }
    }

    private void handleError(String token, FirebaseMessagingException e) {
        String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        if (code.equals("UNREGISTERED") || code.equals("INVALID_ARGUMENT")) {
            log.info("Removing invalid FCM token");
            fcmTokenQueryPort.invalidateToken(token);
        }
    }
}
