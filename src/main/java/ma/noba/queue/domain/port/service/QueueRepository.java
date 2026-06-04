package ma.noba.queue.domain.port.service;

import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueRepository {

    Queue save(Queue queue);

    Optional<Queue> findById(UUID id);

    Optional<Queue> findByProviderId(UUID providerId);

    List<Queue> findAll();

    void delete(Queue queue);

    long countByStatusIn(List<QueueStatus> statuses);

    long countAllMembers();
}
