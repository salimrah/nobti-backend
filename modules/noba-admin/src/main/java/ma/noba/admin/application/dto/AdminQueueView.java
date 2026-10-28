package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminQueueView(
        UUID id,
        UUID providerId,
        String providerName,
        String status,
        long waiting,
        long serving,
        long members,
        Instant updatedAt) {
}
