package ma.noba.queue.domain.port.service;

import java.util.Optional;

/** Resolves a provider-service id to its display name (used in queue member read models). */
public interface ProviderServiceNamePort {

    Optional<String> findServiceName(String serviceId);
}
