package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminQueueDetailView(
        UUID id,
        UUID providerId,
        String providerName,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<Member> members) {

    public record Member(
            UUID id,
            UUID clientId,
            String clientName,
            int position,
            String status,
            Instant joinedAt,
            Instant estimatedStartTime) {
    }
}
