package ma.noba.queue.infra.adapter;

import ma.noba.queue.application.service.QueueAnalyticsService;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.port.QueueAnalyticsQueryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class QueueAnalyticsQueryPortAdapter implements QueueAnalyticsQueryPort {

    private final QueueAnalyticsService queueAnalyticsService;

    public QueueAnalyticsQueryPortAdapter(QueueAnalyticsService queueAnalyticsService) {
        this.queueAnalyticsService = queueAnalyticsService;
    }

    @Override
    public Optional<QueueAnalyticsData> findTodayAnalytics(UUID providerId) {
        try {
            var response = queueAnalyticsService.getTodayAnalytics(providerId);
            return Optional.of(new QueueAnalyticsData(
                    response.customersServedToday(),
                    response.avgWaitMinutes(),
                    response.currentQueueLength(),
                    response.peakHour(),
                    response.hourlyDistribution()
            ));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
