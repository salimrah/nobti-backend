package ma.noba.queue.infra.listener;

import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.model.QueueAnalyticsSnapshot;
import ma.noba.queue.domain.port.service.QueueAnalyticsSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class QueueAnalyticsSnapshotListener {

    private static final Logger log = LoggerFactory.getLogger(QueueAnalyticsSnapshotListener.class);

    private final QueueAnalyticsSnapshotRepository snapshotRepository;

    public QueueAnalyticsSnapshotListener(QueueAnalyticsSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientJoined(ClientJoinedQueueEvent event) {
        log.debug("Client {} joined queue for provider {} - ensuring analytics snapshot exists",
                event.clientId(), event.providerId());
        getOrCreateSnapshot(event.providerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClientLeft(ClientLeftQueueEvent event) {
        log.debug("Client {} left queue for provider {} - no analytics update (left != served)",
                event.clientId(), event.providerId());
    }

    private QueueAnalyticsSnapshot getOrCreateSnapshot(UUID providerId) {
        LocalDate today = LocalDate.now();
        return snapshotRepository.findByProviderIdAndDate(providerId, today)
                .orElseGet(() -> {
                    QueueAnalyticsSnapshot snapshot = new QueueAnalyticsSnapshot(providerId, today);
                    snapshotRepository.save(snapshot);
                    return snapshot;
                });
    }
}
