package ma.noba.provider.infra.listener;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class ProviderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ProviderNotificationListener.class);

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenQueryPort fcmTokenQueryPort;

    public ProviderNotificationListener(
            Optional<FirebaseMessaging> firebaseMessaging,
            FcmTokenQueryPort fcmTokenQueryPort) {
        this.firebaseMessaging = firebaseMessaging.orElse(null);
        this.fcmTokenQueryPort = fcmTokenQueryPort;
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderStatusChanged(ProviderStatusChangedEvent event) {
        if (firebaseMessaging == null) {
            log.info("[MOCK] Provider status notification for user {}: {} -> {}",
                    event.userId(), event.oldStatus(), event.newStatus());
            return;
        }

        String token = fcmTokenQueryPort.findFcmTokenByUserId(event.userId()).orElse(null);
        if (token == null) {
            log.warn("No FCM token found for user {}", event.userId());
            return;
        }

        String title;
        String body;
        if (event.newStatus() == ProviderStatus.ACTIVE) {
            title = "Profile Approved!";
            body = "Your provider profile has been approved. You can now start managing your queue.";
        } else if (event.newStatus() == ProviderStatus.REJECTED) {
            title = "Profile Not Approved";
            body = "Your provider profile was not approved. Please contact support for more information.";
        } else {
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "provider_status_changed")
                    .putData("status", event.newStatus().name())
                    .putData("userId", event.userId().toString())
                    .build();

            firebaseMessaging.send(message);
            log.info("Provider status notification sent to user {}: {}", event.userId(), event.newStatus());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send provider status notification to user {}", event.userId(), e);
            if (isTokenInvalid(e)) {
                fcmTokenQueryPort.invalidateToken(token);
            }
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return code.equals("UNREGISTERED") || code.equals("INVALID_ARGUMENT");
    }
}
