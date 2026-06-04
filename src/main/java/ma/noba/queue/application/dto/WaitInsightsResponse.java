package ma.noba.queue.application.dto;

import java.util.List;
import java.util.Map;

public record WaitInsightsResponse(
        double averageWaitMinutes,
        double currentWaitMinutes,
        String peakHour,
        String bestTimeToVisit,
        int totalDataPoints,
        String trend,
        Map<String, Double> dailyBreakdown,
        Map<String, Double> hourlyBreakdown
) {}
