package ma.noba.queue.domain.port.service;

import java.util.UUID;

/** Queue rules configured per provider (with safe defaults when unset). */
public interface ProviderQueuePolicyPort {

    QueuePolicy policy(UUID providerId);

    record QueuePolicy(int maxPartySize, boolean walkInsEnabled) {}
}
