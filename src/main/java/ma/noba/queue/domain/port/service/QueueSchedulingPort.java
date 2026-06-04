package ma.noba.queue.domain.port.service;

import java.time.Instant;
import java.util.UUID;

public interface QueueSchedulingPort {
    void rescheduleTurnReminder(UUID clientId, UUID providerId, int position, Instant estimatedStartTime);
}
