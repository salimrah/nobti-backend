package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminAuditResponse(
        long id,
        UUID actorId,
        String actorLabel,
        String action,
        String targetType,
        UUID targetId,
        Map<String, Object> metadata,
        Instant createdAt) {
}
