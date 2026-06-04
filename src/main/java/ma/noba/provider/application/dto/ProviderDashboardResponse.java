package ma.noba.provider.application.dto;

import java.util.List;
import java.util.Map;

public record ProviderDashboardResponse(
        int todayCustomers,
        double avgRating,
        double avgRatingTrend7Day,
        int currentQueueLength,
        int estimatedWaitMinutes,
        String peakHour,
        Map<String, Integer> hourlyDistribution,
        List<TopService> topServices
) {
    public record TopService(
            String name,
            int timesUsed,
            double avgDurationMinutes
    ) {}
}
