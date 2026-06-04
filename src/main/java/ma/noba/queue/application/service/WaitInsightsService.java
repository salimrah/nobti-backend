package ma.noba.queue.application.service;

import ma.noba.queue.application.dto.WaitInsightsResponse;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueAnalyticsSnapshot;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.port.service.QueueAnalyticsSnapshotRepository;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class WaitInsightsService {

    private final QueueRepository queueRepository;
    private final QueueAnalyticsSnapshotRepository snapshotRepository;

    public WaitInsightsService(QueueRepository queueRepository, QueueAnalyticsSnapshotRepository snapshotRepository) {
        this.queueRepository = queueRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public WaitInsightsResponse getWaitInsights(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElse(null);

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<QueueAnalyticsSnapshot> snapshots = snapshotRepository
                .findByProviderIdAndDateBetween(providerId, thirtyDaysAgo, today);

        int totalDataPoints = snapshots.size();

        Map<DayOfWeek, List<Double>> waitByDay = new HashMap<>();
        Map<String, List<Double>> waitByHour = new HashMap<>();
        double totalWaitSum = 0;
        int totalWaitCount = 0;

        for (QueueAnalyticsSnapshot snap : snapshots) {
            DayOfWeek day = snap.getDate().getDayOfWeek();
            waitByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(snap.getAvgWaitTimeMinutes());
            totalWaitSum += snap.getAvgWaitTimeMinutes();
            totalWaitCount++;
        }

        if (queue != null) {
            List<QueueMember> completed = queue.getMembers().stream()
                    .filter(m -> m.getStatus() == QueueMemberStatus.COMPLETED
                            && m.getEstimatedStartTime() != null
                            && m.getCreatedAt().isAfter(thirtyDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .toList();

            for (QueueMember m : completed) {
                String hour = m.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .truncatedTo(ChronoUnit.HOURS)
                        .toString();
                double wait = ChronoUnit.MINUTES.between(m.getCreatedAt(), m.getEstimatedStartTime());
                waitByHour.computeIfAbsent(hour, k -> new ArrayList<>()).add((double) wait);
                DayOfWeek day = m.getCreatedAt().atZone(ZoneId.systemDefault()).getDayOfWeek();
                waitByDay.computeIfAbsent(day, k -> new ArrayList<>()).add((double) wait);
                totalWaitSum += wait;
                totalWaitCount++;
                totalDataPoints++;
            }
        }

        Map<String, Double> dailyBreakdown = new LinkedHashMap<>();
        String[] dayNames = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        for (String dayName : dayNames) {
            DayOfWeek day = DayOfWeek.valueOf(dayName);
            List<Double> waits = waitByDay.getOrDefault(day, List.of());
            double avg = waits.isEmpty() ? 0.0 : waits.stream().mapToDouble(d -> d).average().orElse(0.0);
            dailyBreakdown.put(dayName, Math.round(avg * 10.0) / 10.0);
        }

        Map<String, Double> hourlyBreakdown = new TreeMap<>();
        for (Map.Entry<String, List<Double>> entry : waitByHour.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(d -> d).average().orElse(0.0);
            hourlyBreakdown.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);
        }

        String peakHour = hourlyBreakdown.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String bestTimeToVisit = hourlyBreakdown.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + Math.round(e.getValue()) + " min avg)")
                .orElse("Anytime");

        String bestDay = dailyBreakdown.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(e -> {
                    String label = switch (e.getKey()) {
                        case "MONDAY" -> "Mon";
                        case "TUESDAY" -> "Tue";
                        case "WEDNESDAY" -> "Wed";
                        case "THURSDAY" -> "Thu";
                        case "FRIDAY" -> "Fri";
                        case "SATURDAY" -> "Sat";
                        case "SUNDAY" -> "Sun";
                        default -> e.getKey();
                    };
                    return label + " (" + Math.round(e.getValue()) + " min)";
                })
                .orElse("Anytime");

        double averageWaitMinutes = totalWaitCount > 0 ? Math.round((totalWaitSum / totalWaitCount) * 10.0) / 10.0 : 0.0;

        double currentWaitMinutes = 0.0;
        if (queue != null) {
            List<QueueMember> waiting = queue.getActiveMembers().stream()
                    .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                    .toList();
            if (!waiting.isEmpty()) {
                currentWaitMinutes = ChronoUnit.MINUTES.between(Instant.now(), waiting.getFirst().getEstimatedStartTime());
                currentWaitMinutes = Math.max(0, currentWaitMinutes);
            }
        }

        String trend = "stable";
        if (snapshots.size() >= 7) {
            int half = snapshots.size() / 2;
            double olderAvg = snapshots.subList(0, half).stream()
                    .mapToDouble(QueueAnalyticsSnapshot::getAvgWaitTimeMinutes)
                    .average().orElse(0.0);
            double recentAvg = snapshots.subList(half, snapshots.size()).stream()
                    .mapToDouble(QueueAnalyticsSnapshot::getAvgWaitTimeMinutes)
                    .average().orElse(0.0);
            if (recentAvg > olderAvg * 1.1) trend = "increasing";
            else if (recentAvg < olderAvg * 0.9) trend = "decreasing";
        }

        return new WaitInsightsResponse(
                averageWaitMinutes,
                currentWaitMinutes,
                peakHour,
                bestDay,
                totalDataPoints,
                trend,
                dailyBreakdown,
                hourlyBreakdown
        );
    }
}
