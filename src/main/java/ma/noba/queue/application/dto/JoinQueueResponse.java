package ma.noba.queue.application.dto;

import ma.noba.queue.domain.model.QueuePriority;

import java.time.Instant;

public record JoinQueueResponse(
        String queueId,
        String memberId,
        int position,
        QueuePriority priority,
        Instant estimatedStartTime,
        int estimatedWaitMinutes
) {}
