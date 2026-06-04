package ma.noba.provider.application.service;

import ma.noba.provider.application.dto.ProviderDashboardResponse;
import ma.noba.provider.application.dto.ProviderDashboardResponse.TopService;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.domain.model.ProviderService;
import ma.noba.shared.domain.port.QueueAnalyticsQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProviderDashboardService {

    private final ProviderApplicationService providerService;
    private final QueueAnalyticsQueryPort queueAnalyticsQueryPort;
    private final ma.noba.provider.domain.port.service.ProviderRatingRepository ratingRepository;

    public ProviderDashboardService(ProviderApplicationService providerService,
                                    QueueAnalyticsQueryPort queueAnalyticsQueryPort,
                                    ma.noba.provider.domain.port.service.ProviderRatingRepository ratingRepository) {
        this.providerService = providerService;
        this.queueAnalyticsQueryPort = queueAnalyticsQueryPort;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public ProviderDashboardResponse getDashboard(UUID userId) {
        ProviderResponse provider = providerService.getMyProvider(userId);

        var analytics = queueAnalyticsQueryPort.findTodayAnalytics(provider.id())
                .orElse(new QueueAnalyticsQueryPort.QueueAnalyticsData(0, 0.0, 0, "N/A", java.util.Map.of()));

        double avgRating = ratingRepository.getAverageRating(provider.id());
        double avgRatingTrend = avgRating;

        List<TopService> topServices = List.of();

        return new ProviderDashboardResponse(
                analytics.customersServedToday(),
                avgRating,
                avgRatingTrend,
                analytics.currentQueueLength(),
                (int) Math.round(analytics.avgWaitMinutes()),
                analytics.peakHour(),
                analytics.hourlyDistribution(),
                topServices
        );
    }
}
