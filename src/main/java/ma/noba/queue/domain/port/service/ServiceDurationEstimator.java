package ma.noba.queue.domain.port.service;

import java.time.Duration;
import java.util.UUID;

public interface ServiceDurationEstimator {

    Duration estimate(UUID providerId, String serviceId);

    Duration getDefaultDuration();
}
