package ma.noba.queue.application.dto;

import java.time.Instant;

public record WaitlistEntryResponse(
        String id,
        String providerId,
        String providerName,
        String clientId,
        String clientName,
        String status,
        int position,
        Instant createdAt,
        Instant notifiedAt
) {}
