package ma.noba.queue.application.dto;

import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueuePriority;

import java.time.Instant;

public record QueueMemberResponse(
        String id,
        String clientId,
        String clientName,
        String serviceId,
        String employeeId,
        int position,
        QueueMemberStatus status,
        QueuePriority priority,
        Instant estimatedStartTime,
        Instant actualStartTime,
        double actualDurationMinutes,
        int estimatedWaitMinutes,
        String groupId,
        int groupSize,
        String providerId,
        int queueSize,
        WaitEstimate estimate
) {}
