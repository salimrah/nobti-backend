package ma.noba.queue.infra;

import ma.noba.queue.domain.model.QueueWaitlistEntry;
import ma.noba.queue.domain.model.QueueWaitlistStatus;
import ma.noba.queue.domain.port.service.QueueWaitlistRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaQueueWaitlistRepository
        extends JpaRepository<QueueWaitlistEntry, UUID>, QueueWaitlistRepository {

    @Query("SELECT e FROM QueueWaitlistEntry e WHERE e.providerId = :providerId AND e.clientId = :clientId "
            + "AND e.status IN (ma.noba.queue.domain.model.QueueWaitlistStatus.WAITING, ma.noba.queue.domain.model.QueueWaitlistStatus.NOTIFIED)")
    Optional<QueueWaitlistEntry> findActiveByProviderAndClient(@Param("providerId") UUID providerId,
                                                               @Param("clientId") UUID clientId);

    @Query("SELECT e FROM QueueWaitlistEntry e WHERE e.providerId = :providerId "
            + "AND e.status IN (ma.noba.queue.domain.model.QueueWaitlistStatus.WAITING, ma.noba.queue.domain.model.QueueWaitlistStatus.NOTIFIED) "
            + "ORDER BY e.createdAt ASC")
    List<QueueWaitlistEntry> findActiveByProvider(@Param("providerId") UUID providerId);

    @Query("SELECT e FROM QueueWaitlistEntry e WHERE e.clientId = :clientId "
            + "AND e.status IN (ma.noba.queue.domain.model.QueueWaitlistStatus.WAITING, ma.noba.queue.domain.model.QueueWaitlistStatus.NOTIFIED) "
            + "ORDER BY e.createdAt ASC")
    List<QueueWaitlistEntry> findActiveByClient(@Param("clientId") UUID clientId);
}
