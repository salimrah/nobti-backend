package ma.noba.queue.application.dto;

import java.time.Duration;
import java.util.Map;

public record QueueAnalyticsResponse(
        int customersServedToday,
        double avgWaitMinutes,
        double avgServiceDurationMinutes,
        int currentQueueLength,
        String peakHour,
        Map<String, Integer> hourlyDistribution
) {}
