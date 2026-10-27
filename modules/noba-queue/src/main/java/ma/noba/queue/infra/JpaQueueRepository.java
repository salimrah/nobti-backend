package ma.noba.queue.infra;

import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.port.service.QueueRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JpaQueueRepository extends JpaRepository<Queue, UUID>, QueueRepository {

    @Query("SELECT DISTINCT q FROM Queue q LEFT JOIN FETCH q.members WHERE q.providerId = :providerId")
    Optional<Queue> findByProviderId(UUID providerId);

    @Query("SELECT COUNT(m) FROM Queue q JOIN q.members m")
    long countAllMembers();
}
