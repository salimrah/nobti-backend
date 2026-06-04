package ma.noba.queue.application.dto;

import ma.noba.queue.domain.model.QueueStatus;

import java.time.Instant;

public record QueueStatusResponse(
        String queueId,
        QueueStatus status,
        int waitingCount,
        int activeCount,
        Instant nextEstimatedTime
) {}
