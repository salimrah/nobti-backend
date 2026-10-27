package ma.noba.provider.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.noba.queue.domain.port.service.ProviderServiceNamePort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ProviderServiceNameAdapter implements ProviderServiceNamePort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<String> findServiceName(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return Optional.empty();
        }
        try {
            var names = entityManager.createQuery(
                            "SELECT s.name FROM ProviderService s WHERE s.id = :id", String.class)
                    .setParameter("id", UUID.fromString(serviceId))
                    .getResultList();
            return names.isEmpty() ? Optional.empty() : Optional.of(names.get(0));
        } catch (Exception e) {
            // Unknown / non-UUID id — fall back to no name rather than failing the queue read.
            return Optional.empty();
        }
    }
}
