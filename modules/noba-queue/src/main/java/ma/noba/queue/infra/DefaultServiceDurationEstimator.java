package ma.noba.queue.infra;

import ma.noba.queue.domain.port.service.ServiceDurationEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnMissingBean(ServiceDurationEstimator.class)
public class DefaultServiceDurationEstimator implements ServiceDurationEstimator {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    @Override
    public Duration estimate(UUID providerId, String serviceId) {
        return DEFAULT_DURATION;
    }

    @Override
    public Duration getDefaultDuration() {
        return DEFAULT_DURATION;
    }
}
