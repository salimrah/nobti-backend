package ma.noba.provider.infra.listener;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.model.ProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;

/**
 * Provider approval/rejection becomes a first-class notification (inbox + push) through the
 * notification module instead of calling Firebase directly.
 */
@Component
@RequiredArgsConstructor
public class ProviderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ProviderNotificationListener.class);

    private final NotificationPort notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderStatusChanged(ProviderStatusChangedEvent event) {
        if (event.newStatus() != ProviderStatus.ACTIVE && event.newStatus() != ProviderStatus.REJECTED) {
            return;
        }
        String title;
        String body;
        if (event.newStatus() == ProviderStatus.ACTIVE) {
            title = "Profile Approved!";
            body = "Your provider profile has been approved. You can now start managing your queue.";
        } else {
            title = "Profile Not Approved";
            body = "Your provider profile was not approved. Please contact support for more information.";
        }

        try {
            notifications.send(new NotificationPort.Command(
                    event.userId(),
                    "provider.status_changed",
                    NotificationPort.Category.TRANSACTIONAL,
                    2,
                    title,
                    body,
                    Map.of("status", event.newStatus().name(),
                            "providerId", event.providerId().toString()),
                    "provider.status:" + event.providerId() + ":" + event.newStatus(),
                    null,
                    Set.of(NotificationPort.Channel.PUSH),
                    null,
                    null));
        } catch (Exception e) {
            log.error("Failed to record provider status notification for user {}", event.userId(), e);
        }
    }
}
