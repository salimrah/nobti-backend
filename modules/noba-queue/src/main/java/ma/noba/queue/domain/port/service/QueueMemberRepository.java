package ma.noba.queue.domain.port.service;

import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueMemberRepository {

    QueueMember save(QueueMember member);

    Optional<QueueMember> findById(UUID id);

    List<QueueMember> findByClientId(UUID clientId);

    List<QueueMember> findByQueueId(UUID queueId);

    Page<QueueMember> findByQueueId(UUID queueId, Pageable pageable);

    /** Paged query filtered to the given statuses (e.g. only active members). */
    Page<QueueMember> findByQueueIdAndStatusIn(UUID queueId, Collection<QueueMemberStatus> statuses, Pageable pageable);

    List<QueueMember> findDueForReminder(QueueMemberStatus status, Instant threshold);

    List<QueueMember> findByStatusInAndCreatedAtBefore(List<QueueMemberStatus> statuses, Instant cutoff);
}
