package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminProviderDocumentView(
        UUID id,
        UUID providerId,
        String providerName,
        String type,
        String fileName,
        String contentType,
        long sizeBytes,
        String status,
        String reviewerNote,
        Instant createdAt,
        Instant reviewedAt) {
}
