package ma.noba.queue.infra;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import ma.noba.config.ResilienceConfig;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.notification.provider", havingValue = "fcm")
public class FcmQueueNotificationPort implements QueueNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(FcmQueueNotificationPort.class);

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenQueryPort fcmTokenQueryPort;

    public FcmQueueNotificationPort(
            FirebaseMessaging firebaseMessaging,
            FcmTokenQueryPort fcmTokenQueryPort) {
        this.firebaseMessaging = firebaseMessaging;
        this.fcmTokenQueryPort = fcmTokenQueryPort;
    }

    @Override
    @CircuitBreaker(name = ResilienceConfig.EXTERNAL_SERVICE, fallbackMethod = "notifyTurnStartFallback")
    @Retry(name = ResilienceConfig.EXTERNAL_SERVICE)
    public void notifyTurnStart(UUID clientId, UUID providerId, int position) {
        String token = getFcmToken(clientId);
        if (token == null) {
            log.warn("No FCM token found for client {}", clientId);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("Your turn is now!")
                            .setBody("It's your turn at the service provider.")
                            .build())
                    .putData("type", "turn_start")
                    .putData("clientId", clientId.toString())
                    .putData("providerId", providerId.toString())
                    .build();

            firebaseMessaging.send(message);
            log.info("Turn start notification sent to client {}", clientId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send turn start notification to client {}", clientId, e);
            handleMessagingError(token, e);
        }
    }

    public void notifyTurnStartFallback(UUID clientId, UUID providerId, int position, Throwable t) {
        log.error("FCM turn start notification failed for client {} — circuit breaker fallback: {}", clientId, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = ResilienceConfig.EXTERNAL_SERVICE, fallbackMethod = "notifyEstimatedWaitFallback")
    @Retry(name = ResilienceConfig.EXTERNAL_SERVICE)
    public void notifyEstimatedWait(UUID clientId, UUID providerId, int position) {
        String token = getFcmToken(clientId);
        if (token == null) {
            log.warn("No FCM token found for client {}", clientId);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("You've joined the queue")
                            .setBody("Your estimated position is #" + position)
                            .build())
                    .putData("type", "estimated_wait")
                    .putData("clientId", clientId.toString())
                    .putData("providerId", providerId.toString())
                    .putData("position", String.valueOf(position))
                    .build();

            firebaseMessaging.send(message);
            log.info("Estimated wait notification sent to client {}", clientId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send estimated wait notification to client {}", clientId, e);
            handleMessagingError(token, e);
        }
    }

    public void notifyEstimatedWaitFallback(UUID clientId, UUID providerId, int position, Throwable t) {
        log.error("FCM estimated wait notification failed for client {} — circuit breaker fallback: {}", clientId, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = ResilienceConfig.EXTERNAL_SERVICE, fallbackMethod = "notifyWaitlistOpenFallback")
    @Retry(name = ResilienceConfig.EXTERNAL_SERVICE)
    public void notifyWaitlistOpen(UUID clientId, UUID providerId) {
        String token = getFcmToken(clientId);
        if (token == null) {
            log.warn("No FCM token found for client {}", clientId);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("A spot just opened")
                            .setBody("The queue is open — join now before it fills up.")
                            .build())
                    .putData("type", "waitlist_open")
                    .putData("clientId", clientId.toString())
                    .putData("providerId", providerId.toString())
                    .build();

            firebaseMessaging.send(message);
            log.info("Waitlist open notification sent to client {}", clientId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send waitlist notification to client {}", clientId, e);
            handleMessagingError(token, e);
        }
    }

    public void notifyWaitlistOpenFallback(UUID clientId, UUID providerId, Throwable t) {
        log.error("FCM waitlist notification failed for client {} — circuit breaker fallback: {}", clientId, t.getMessage());
    }

    private String getFcmToken(UUID clientId) {
        return fcmTokenQueryPort.findFcmTokenByUserId(clientId).orElse(null);
    }

    private void handleMessagingError(String token, FirebaseMessagingException e) {
        if (isTokenInvalid(e)) {
            log.info("Removing invalid FCM token for client");
            fcmTokenQueryPort.invalidateToken(token);
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return code.equals("UNREGISTERED") || code.equals("INVALID_ARGUMENT");
    }
}
