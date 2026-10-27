package ma.noba.shared.domain.port;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface QueueAnalyticsQueryPort {
    Optional<QueueAnalyticsData> findTodayAnalytics(UUID providerId);

    record QueueAnalyticsData(
            int customersServedToday,
            double avgWaitMinutes,
            int currentQueueLength,
            String peakHour,
            Map<String, Integer> hourlyDistribution
    ) {}
}
