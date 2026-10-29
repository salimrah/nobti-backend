package ma.noba.admin.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.admin.application.dto.AdminAuditResponse;
import ma.noba.admin.domain.model.AdminAuditEvent;
import ma.noba.admin.domain.port.AdminAuditRepository;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminAuditPort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Records and reads the administrative audit trail. Writes participate in the caller's transaction,
 * so an action that fails leaves no audit entry.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService implements AdminAuditPort {

    private final AdminAuditRepository repository;
    private final AdminUserDirectoryPort directory;

    @Override
    @Transactional
    public void record(UUID actorId, String actorLabel, String action, String targetType,
                       UUID targetId, Map<String, Object> metadata) {
        repository.persist(AdminAuditEvent.of(
                actorId,
                resolveLabel(actorId, actorLabel),
                action.toUpperCase(Locale.ROOT),
                targetType.toUpperCase(Locale.ROOT),
                targetId,
                metadata));
    }

    /** Tokens carry no contact details, so anonymous labels fall back to the profile name. */
    private String resolveLabel(UUID actorId, String actorLabel) {
        if (actorLabel != null && !actorLabel.isBlank()) {
            return actorLabel;
        }
        try {
            return directory.displayNames(List.of(actorId)).get(actorId);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAuditResponse> search(String action, String targetType, int page, int size) {
        Page<AdminAuditEvent> result = repository.search(
                normalize(action),
                normalize(targetType),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PagedResponse.of(
                result.getContent().stream()
                        .map(event -> new AdminAuditResponse(
                                event.getId(),
                                event.getActorId(),
                                event.getActorLabel(),
                                event.getAction(),
                                event.getTargetType(),
                                event.getTargetId(),
                                event.getMetadata(),
                                event.getCreatedAt()))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
