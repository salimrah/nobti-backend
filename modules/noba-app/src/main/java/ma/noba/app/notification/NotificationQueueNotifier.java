package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Queue notifications now go through the notification subsystem, so every turn/wait/waitlist
 * message is recorded, auditable and visible in the in-app inbox. Delivery is push via the
 * provider-neutral gateway.
 */
@Component
@RequiredArgsConstructor
public class NotificationQueueNotifier implements QueueNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueueNotifier.class);

    private final NotificationPort notifications;

    @Override
    public void notifyTurnStart(UUID clientId, UUID providerId, int position) {
        send(clientId, "queue.turn_start", 2,
                "It's your turn!",
                "It's your turn at the service provider. Please arrive within 5 minutes.",
                Map.of("type", "turn_start", "providerId", providerId.toString(), "position", position),
                "queue.turn:" + providerId + ":" + clientId + ":" + position);
    }

    @Override
    public void notifyEstimatedWait(UUID clientId, UUID providerId, int estimatedWaitMinutes) {
        send(clientId, "queue.estimated_wait", 1,
                "Queue update",
                "Estimated wait: " + estimatedWaitMinutes + " min.",
                Map.of("type", "estimated_wait", "providerId", providerId.toString(),
                        "minutes", estimatedWaitMinutes),
                "queue.wait:" + providerId + ":" + clientId + ":" + estimatedWaitMinutes);
    }

    @Override
    public void notifyWaitlistOpen(UUID clientId, UUID providerId) {
        send(clientId, "queue.waitlist_open", 2,
                "A spot is open",
                "The queue you were waiting for is now open. Join now!",
                Map.of("type", "waitlist_open", "providerId", providerId.toString()),
                "queue.waitlist:" + providerId + ":" + clientId);
    }

    private void send(UUID userId, String type, int priority, String title, String body,
                      Map<String, Object> data, String dedupeKey) {
        try {
            notifications.send(new NotificationPort.Command(
                    userId, type, NotificationPort.Category.TRANSACTIONAL, priority,
                    title, body, data, dedupeKey, null,
                    Set.of(NotificationPort.Channel.PUSH), null, null));
        } catch (Exception e) {
            // A notification failure must never break the queue operation.
            log.error("Failed to record {} notification for user {}", type, userId, e);
        }
    }
}
