package ma.noba.queue.application.service;

import ma.noba.queue.application.dto.QueueAnalyticsResponse;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QueueAnalyticsService {

    private final QueueRepository queueRepository;

    public QueueAnalyticsService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @Transactional(readOnly = true)
    public QueueAnalyticsResponse getTodayAnalytics(UUID providerId) {
        return computeAnalytics(providerId, Instant.now().truncatedTo(ChronoUnit.DAYS));
    }

    @Transactional(readOnly = true)
    public QueueAnalyticsResponse getWeekAnalytics(UUID providerId) {
        return computeAnalytics(providerId, Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private QueueAnalyticsResponse computeAnalytics(UUID providerId, Instant since) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No queue for provider"));

        List<QueueMember> allMembers = queue.getMembers();

        List<QueueMember> filteredMembers = allMembers.stream()
                .filter(m -> m.getCreatedAt().isAfter(since))
                .toList();

        List<QueueMember> completed = filteredMembers.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.COMPLETED)
                .toList();

        int customersServed = completed.size();

        double avgWait = completed.stream()
                .filter(m -> m.getEstimatedStartTime() != null)
                .mapToLong(m -> ChronoUnit.MINUTES.between(m.getCreatedAt(), m.getEstimatedStartTime()))
                .average()
                .orElse(0.0);

        double avgServiceDuration = completed.stream()
                .filter(m -> m.getActualDuration() != null)
                .mapToDouble(m -> m.getActualDuration().toMinutes())
                .average()
                .orElse(0.0);

        int currentLength = queue.getActiveMembers().size();

        Map<String, Integer> hourlyDistribution = buildHourlyDistribution(filteredMembers);
        String peakHour = hourlyDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new QueueAnalyticsResponse(
                customersServed,
                Math.round(avgWait * 100.0) / 100.0,
                Math.round(avgServiceDuration * 100.0) / 100.0,
                currentLength,
                peakHour,
                hourlyDistribution
        );
    }

    private Map<String, Integer> buildHourlyDistribution(List<QueueMember> members) {
        Map<String, Integer> distribution = new HashMap<>();
        for (QueueMember member : members) {
            String hour = member.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .truncatedTo(ChronoUnit.HOURS)
                    .toString();
            distribution.merge(hour, 1, Integer::sum);
        }
        return distribution;
    }
}
