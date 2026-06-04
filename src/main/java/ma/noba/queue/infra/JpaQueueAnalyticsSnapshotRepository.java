package ma.noba.queue.infra;

import ma.noba.queue.domain.model.QueueAnalyticsSnapshot;
import ma.noba.queue.domain.port.service.QueueAnalyticsSnapshotRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaQueueAnalyticsSnapshotRepository extends JpaRepository<QueueAnalyticsSnapshot, UUID>, QueueAnalyticsSnapshotRepository {

    Optional<QueueAnalyticsSnapshot> findByProviderIdAndDate(UUID providerId, LocalDate date);

    List<QueueAnalyticsSnapshot> findByProviderIdAndDateBetween(UUID providerId, LocalDate start, LocalDate end);

    List<QueueAnalyticsSnapshot> findByProviderIdOrderByDateDesc(UUID providerId);
}
