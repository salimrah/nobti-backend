package ma.noba.shared.domain.port;

import java.util.Map;
import java.util.UUID;

/**
 * Audit hook for modules that perform an administrative action but cannot depend on the admin
 * module. The admin module implements it.
 */
public interface AdminAuditPort {

    void record(UUID actorId, String actorLabel, String action, String targetType,
                UUID targetId, Map<String, Object> metadata);
}
