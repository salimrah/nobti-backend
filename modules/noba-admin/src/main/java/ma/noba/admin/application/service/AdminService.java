package ma.noba.admin.application.service;

import ma.noba.admin.application.dto.AdminAuditResponse;
import ma.noba.admin.application.dto.AdminQueueDetailView;
import ma.noba.admin.application.dto.AdminQueueView;
import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminQueuePort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import ma.noba.shared.domain.port.AdminProviderPort;
import ma.noba.shared.domain.port.AdminUserPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminIdentityPort adminIdentityPort;
    private final AdminQueuePort adminQueuePort;
    private final AdminUserPort adminUserPort;
    private final AdminProviderPort adminProviderPort;
    private final AdminUserDirectoryPort adminUserDirectoryPort;
    private final AdminAuditService auditService;

    public AdminService(AdminIdentityPort adminIdentityPort, AdminQueuePort adminQueuePort,
                        AdminUserPort adminUserPort, AdminProviderPort adminProviderPort,
                        AdminUserDirectoryPort adminUserDirectoryPort, AdminAuditService auditService) {
        this.adminIdentityPort = adminIdentityPort;
        this.adminQueuePort = adminQueuePort;
        this.adminUserPort = adminUserPort;
        this.adminProviderPort = adminProviderPort;
        this.adminUserDirectoryPort = adminUserDirectoryPort;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                adminIdentityPort.countUsers(),
                adminIdentityPort.countActiveUsers(),
                adminIdentityPort.countSuspendedUsers(),
                adminUserPort.countProviders(),
                adminProviderPort.countPendingProviders(),
                adminQueuePort.countActiveQueues(),
                adminQueuePort.countTotalQueueMembers()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserDirectoryPort.UserRow> searchUsers(
            String query, String status, String role, int page, int size) {
        UserStatus parsed = parseStatus(status);
        return adminUserDirectoryPort.searchUsers(
                query == null ? "" : query,
                parsed,
                role == null ? "" : role,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminProviderPort.ProviderRow> searchProviders(
            String query, String status, int page, int size) {
        return adminProviderPort.searchProviders(
                query == null ? "" : query,
                status == null ? "" : status,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100));
    }

    private static UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional
    public void suspendUser(UUID actorId, String actorLabel, UUID userId) {
        String previous = adminIdentityPort.findUser(userId).map(AdminIdentityPort.UserState::status).orElse(null);
        adminIdentityPort.suspendUser(userId);
        auditService.record(actorId, resolveActor(actorId, actorLabel), "USER_SUSPENDED", "USER", userId,
                statusChange(previous, "DISABLED"));
    }

    @Transactional
    public void activateUser(UUID actorId, String actorLabel, UUID userId) {
        String previous = adminIdentityPort.findUser(userId).map(AdminIdentityPort.UserState::status).orElse(null);
        adminIdentityPort.activateUser(userId);
        auditService.record(actorId, resolveActor(actorId, actorLabel), "USER_ACTIVATED", "USER", userId,
                statusChange(previous, "ACTIVE"));
    }

    @Transactional(readOnly = true)
    public List<AdminProviderPort.PendingProvider> pendingProviders() {
        return adminProviderPort.pendingProviders();
    }

    @Transactional
    public void approveProvider(UUID actorId, String actorLabel, UUID providerId) {
        changeProviderStatus(actorId, actorLabel, providerId, "PROVIDER_APPROVED", "ACTIVE");
    }

    @Transactional
    public void rejectProvider(UUID actorId, String actorLabel, UUID providerId) {
        changeProviderStatus(actorId, actorLabel, providerId, "PROVIDER_REJECTED", "REJECTED");
    }

    private void changeProviderStatus(UUID actorId, String actorLabel, UUID providerId,
                                      String action, String newStatus) {
        AdminProviderPort.ProviderState before = adminProviderPort.findProvider(providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found"));
        Map<String, Object> metadata = statusChange(before.status(), newStatus);
        metadata.put("name", before.name());
        if ("ACTIVE".equals(newStatus)) {
            adminProviderPort.approveProvider(providerId);
        } else {
            adminProviderPort.rejectProvider(providerId);
        }
        auditService.record(actorId, resolveActor(actorId, actorLabel), action, "PROVIDER", providerId, metadata);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminQueueView> searchQueues(String status, int page, int size) {
        PagedResponse<AdminQueuePort.QueueRow> rows = adminQueuePort.searchQueues(status, page, size);
        Map<UUID, String> names = adminProviderPort.names(
                rows.content().stream().map(AdminQueuePort.QueueRow::providerId).distinct().toList());
        return PagedResponse.of(
                rows.content().stream()
                        .map(row -> new AdminQueueView(
                                row.id(),
                                row.providerId(),
                                names.getOrDefault(row.providerId(), "Provider"),
                                row.status(),
                                row.waiting(),
                                row.serving(),
                                row.members(),
                                row.updatedAt()))
                        .toList(),
                rows.page(),
                rows.size(),
                rows.totalElements());
    }

    @Transactional(readOnly = true)
    public AdminQueueDetailView queueDetail(UUID queueId) {
        AdminQueuePort.QueueDetail detail = adminQueuePort.queueDetail(queueId)
                .orElseThrow(() -> new NoSuchElementException("Queue not found"));
        Map<UUID, String> names = adminUserDirectoryPort.displayNames(
                detail.members().stream().map(AdminQueuePort.MemberRow::clientId).distinct().toList());
        String providerName = adminProviderPort.names(List.of(detail.providerId()))
                .getOrDefault(detail.providerId(), "Provider");
        return new AdminQueueDetailView(
                detail.id(),
                detail.providerId(),
                providerName,
                detail.status(),
                detail.createdAt(),
                detail.updatedAt(),
                detail.members().stream()
                        .map(member -> new AdminQueueDetailView.Member(
                                member.id(),
                                member.clientId(),
                                names.getOrDefault(member.clientId(), "Client"),
                                member.position(),
                                member.status(),
                                member.joinedAt(),
                                member.estimatedStartTime()))
                        .toList());
    }

    @Transactional
    public void pauseQueue(UUID actorId, String actorLabel, UUID queueId) {
        changeQueueStatus(actorId, actorLabel, queueId, "QUEUE_PAUSED", "PAUSED",
                () -> adminQueuePort.pauseQueue(queueId));
    }

    @Transactional
    public void resumeQueue(UUID actorId, String actorLabel, UUID queueId) {
        changeQueueStatus(actorId, actorLabel, queueId, "QUEUE_RESUMED", "ACTIVE",
                () -> adminQueuePort.resumeQueue(queueId));
    }

    @Transactional
    public void closeQueue(UUID actorId, String actorLabel, UUID queueId) {
        changeQueueStatus(actorId, actorLabel, queueId, "QUEUE_CLOSED", "CLOSED",
                () -> adminQueuePort.closeQueue(queueId));
    }

    @Transactional
    public void advanceQueue(UUID actorId, String actorLabel, UUID queueId) {
        AdminQueuePort.QueueDetail before = adminQueuePort.queueDetail(queueId)
                .orElseThrow(() -> new NoSuchElementException("Queue not found"));
        adminQueuePort.advanceQueue(queueId);
        AdminQueuePort.QueueDetail after = adminQueuePort.queueDetail(queueId).orElse(before);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("providerId", before.providerId().toString());
        metadata.put("previousServing", before.members().stream()
                .filter(member -> "ACTIVE".equals(member.status())).count());
        metadata.put("serving", after.members().stream()
                .filter(member -> "ACTIVE".equals(member.status())).count());
        auditService.record(actorId, resolveActor(actorId, actorLabel), "QUEUE_ADVANCED", "QUEUE", queueId, metadata);
    }

    private void changeQueueStatus(UUID actorId, String actorLabel, UUID queueId, String action,
                                   String newStatus, Runnable change) {
        AdminQueuePort.QueueDetail before = adminQueuePort.queueDetail(queueId)
                .orElseThrow(() -> new NoSuchElementException("Queue not found"));
        change.run();
        Map<String, Object> metadata = statusChange(before.status(), newStatus);
        metadata.put("providerId", before.providerId().toString());
        auditService.record(actorId, resolveActor(actorId, actorLabel), action, "QUEUE", queueId, metadata);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAuditResponse> audit(String action, String targetType, int page, int size) {
        return auditService.search(action, targetType, page, size);
    }

    /** Falls back to the actor's profile name when the token carries no email. */
    private String resolveActor(UUID actorId, String label) {
        if (label != null && !label.isBlank()) {
            return label;
        }
        return adminUserDirectoryPort.displayNames(List.of(actorId))
                .getOrDefault(actorId, actorId.toString());
    }

    private static Map<String, Object> statusChange(String previous, String next) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousStatus", previous);
        metadata.put("newStatus", next);
        return metadata;
    }
}
