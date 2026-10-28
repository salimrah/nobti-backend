package ma.noba.admin.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.admin.application.dto.AdminAuditResponse;
import ma.noba.admin.domain.model.AdminAuditEvent;
import ma.noba.admin.domain.port.AdminAuditRepository;
import ma.noba.shared.application.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Records and reads the administrative audit trail. Writes participate in the caller's transaction,
 * so an action that fails leaves no audit entry.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditRepository repository;

    @Transactional
    public void record(UUID actorId, String actorLabel, String action, String targetType,
                       UUID targetId, Map<String, Object> metadata) {
        repository.persist(AdminAuditEvent.of(
                actorId,
                actorLabel,
                action.toUpperCase(Locale.ROOT),
                targetType.toUpperCase(Locale.ROOT),
                targetId,
                metadata));
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
