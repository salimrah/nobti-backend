package ma.noba.queue.domain.port.service;

import java.util.UUID;

public interface QueueNotificationPort {

    void notifyTurnStart(UUID clientId, UUID providerId, int position);

    void notifyEstimatedWait(UUID clientId, UUID providerId, int estimatedWaitMinutes);

    /** Tells a waitlisted client that the queue is now open / has room. */
    void notifyWaitlistOpen(UUID clientId, UUID providerId);
}
