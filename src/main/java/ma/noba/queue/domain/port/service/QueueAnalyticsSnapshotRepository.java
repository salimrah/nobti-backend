package ma.noba.queue.domain.port.service;

import ma.noba.queue.domain.model.QueueAnalyticsSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueAnalyticsSnapshotRepository {

    QueueAnalyticsSnapshot save(QueueAnalyticsSnapshot snapshot);

    Optional<QueueAnalyticsSnapshot> findByProviderIdAndDate(UUID providerId, LocalDate date);

    List<QueueAnalyticsSnapshot> findByProviderIdAndDateBetween(UUID providerId, LocalDate start, LocalDate end);

    List<QueueAnalyticsSnapshot> findByProviderIdOrderByDateDesc(UUID providerId);
}
