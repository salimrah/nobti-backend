package ma.noba.queue.domain.port.service;

import java.util.UUID;

public interface QueueNotificationPort {

    void notifyTurnStart(UUID clientId, UUID providerId, int position);

    void notifyEstimatedWait(UUID clientId, UUID providerId, int estimatedWaitMinutes);
}
