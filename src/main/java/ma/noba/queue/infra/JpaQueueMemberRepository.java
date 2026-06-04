package ma.noba.queue.infra;

import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaQueueMemberRepository extends JpaRepository<QueueMember, UUID>, QueueMemberRepository {

    List<QueueMember> findByClientId(UUID clientId);

    List<QueueMember> findByQueueId(UUID queueId);

    Page<QueueMember> findByQueueId(UUID queueId, Pageable pageable);

    @Query("SELECT m FROM QueueMember m JOIN FETCH m.queue WHERE m.status = :status AND m.estimatedStartTime <= :threshold")
    List<QueueMember> findDueForReminder(
            @Param("status") QueueMemberStatus status,
            @Param("threshold") Instant threshold);

    List<QueueMember> findByStatusInAndCreatedAtBefore(List<QueueMemberStatus> statuses, Instant cutoff);
}
