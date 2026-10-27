package ma.noba.queue.domain.port.service;

import ma.noba.queue.domain.model.QueueWaitlistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueWaitlistRepository {

    Optional<QueueWaitlistEntry> findActiveByProviderAndClient(UUID providerId, UUID clientId);

    /** Active entries for a provider, oldest first (FIFO). */
    List<QueueWaitlistEntry> findActiveByProvider(UUID providerId);

    /** Active entries for a client. */
    List<QueueWaitlistEntry> findActiveByClient(UUID clientId);

    QueueWaitlistEntry save(QueueWaitlistEntry entry);
}
