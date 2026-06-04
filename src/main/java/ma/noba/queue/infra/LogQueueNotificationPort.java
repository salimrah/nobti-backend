package ma.noba.queue.infra;

import ma.noba.queue.domain.port.service.QueueNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.notification.provider", havingValue = "log", matchIfMissing = true)
public class LogQueueNotificationPort implements QueueNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogQueueNotificationPort.class);

    @Override
    public void notifyTurnStart(UUID clientId, UUID providerId, int position) {
        log.info("[MOCK] Turn start notification for client {} at provider {} (position {})",
                clientId, providerId, position);
    }

    @Override
    public void notifyEstimatedWait(UUID clientId, UUID providerId, int estimatedWaitMinutes) {
        log.info("[MOCK] Estimated wait notification for client {} at provider {} ({} min)",
                clientId, providerId, estimatedWaitMinutes);
    }
}
