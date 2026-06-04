package ma.noba.provider.infra;

import ma.noba.queue.domain.port.service.ServiceDurationEstimator;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.UUID;

@Component
public class ProviderServiceDurationAdapter implements ServiceDurationEstimator {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Duration estimate(UUID providerId, String serviceId) {
        if (serviceId == null) return DEFAULT_DURATION;
        try {
            UUID uuid = UUID.fromString(serviceId);
            Object result = entityManager.createNativeQuery(
                    "SELECT duration FROM provider_service WHERE id = ?")
                    .setParameter(1, uuid)
                    .getSingleResult();
            if (result instanceof String s) {
                return Duration.parse(s);
            }
            return DEFAULT_DURATION;
        } catch (Exception e) {
            return DEFAULT_DURATION;
        }
    }

    @Override
    public Duration getDefaultDuration() {
        return DEFAULT_DURATION;
    }
}
