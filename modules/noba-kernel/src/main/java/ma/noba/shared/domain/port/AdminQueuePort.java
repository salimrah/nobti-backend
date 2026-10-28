package ma.noba.shared.domain.port;

import ma.noba.shared.application.dto.PagedResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminQueuePort {
    long countActiveQueues();

    long countTotalQueueMembers();

    PagedResponse<QueueRow> searchQueues(String status, int page, int size);

    Optional<QueueDetail> queueDetail(UUID queueId);

    void pauseQueue(UUID queueId);

    void resumeQueue(UUID queueId);

    void closeQueue(UUID queueId);

    void advanceQueue(UUID queueId);

    record QueueRow(UUID id, UUID providerId, String status, long waiting, long serving,
                    long members, Instant updatedAt) {
    }

    record MemberRow(UUID id, UUID clientId, int position, String status,
                     Instant joinedAt, Instant estimatedStartTime) {
    }

    record QueueDetail(UUID id, UUID providerId, String status, Instant createdAt,
                       Instant updatedAt, List<MemberRow> members) {
    }
}
