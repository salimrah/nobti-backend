package ma.noba.admin.application.service;

import ma.noba.admin.application.dto.AdminAuditResponse;
import ma.noba.admin.application.dto.AdminPayoutView;
import ma.noba.admin.application.dto.AdminTransactionView;
import ma.noba.admin.application.dto.AdminQueueDetailView;
import ma.noba.admin.application.dto.AdminQueueView;
import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.admin.application.dto.AdminUserDetailView;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminPaymentPort;
import ma.noba.shared.domain.port.AdminQueuePort;
import ma.noba.shared.domain.port.AdminRolePort;
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
    private final AdminPaymentPort adminPaymentPort;
    private final AdminRolePort adminRolePort;

    public AdminService(AdminIdentityPort adminIdentityPort, AdminQueuePort adminQueuePort,
                        AdminUserPort adminUserPort, AdminProviderPort adminProviderPort,
                        AdminUserDirectoryPort adminUserDirectoryPort, AdminAuditService auditService,
                        AdminPaymentPort adminPaymentPort, AdminRolePort adminRolePort) {
        this.adminIdentityPort = adminIdentityPort;
        this.adminQueuePort = adminQueuePort;
        this.adminUserPort = adminUserPort;
        this.adminProviderPort = adminProviderPort;
        this.adminUserDirectoryPort = adminUserDirectoryPort;
        this.auditService = auditService;
        this.adminPaymentPort = adminPaymentPort;
        this.adminRolePort = adminRolePort;
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
    public void suspendUser(UUID actorId, String actorLabel, UUID userId, String reason) {
        String previous = adminIdentityPort.findUser(userId).map(AdminIdentityPort.UserState::status).orElse(null);
        adminIdentityPort.suspendUser(userId);
        Map<String, Object> metadata = statusChange(previous, "DISABLED");
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason.trim());
        }
        auditService.record(actorId, resolveActor(actorId, actorLabel), "USER_SUSPENDED", "USER", userId, metadata);
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
    public AdminUserDetailView userDetail(UUID userId) {
        AdminIdentityPort.UserState user = adminIdentityPort.findUser(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String name = adminUserDirectoryPort.displayNames(List.of(userId))
                .getOrDefault(userId, user.email() != null ? user.email() : "User");
        return new AdminUserDetailView(
                user.id(), name, user.email(), user.phone(), user.status(),
                adminRolePort.rolesOf(userId), user.createdAt());
    }

    /**
     * Replaces a user's roles. Guards: an admin cannot edit their own roles (no self-lockout), and
     * the last remaining administrator cannot be demoted.
     */
    @Transactional
    public AdminUserDetailView updateRoles(UUID actorId, String actorLabel, UUID userId, java.util.Set<String> roles) {
        if (actorId.equals(userId)) {
            throw new IllegalArgumentException("You cannot change your own roles");
        }
        AdminIdentityPort.UserState user = adminIdentityPort.findUser(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        java.util.Set<String> before = adminRolePort.rolesOf(userId);
        java.util.Set<String> next = new java.util.LinkedHashSet<>();
        if (roles != null) {
            roles.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(value -> value.trim().toUpperCase(java.util.Locale.ROOT))
                    .filter(adminRolePort.assignableRoles()::contains)
                    .forEach(next::add);
        }
        // Every account keeps the base role.
        next.add("USER");
        if (before.contains("ADMIN") && !next.contains("ADMIN") && adminRolePort.countAdmins() <= 1) {
            throw new IllegalArgumentException("The last administrator cannot be demoted");
        }
        adminRolePort.replaceRoles(userId, next);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("before", before);
        metadata.put("after", next);
        metadata.put("name", user.email());
        auditService.record(actorId, resolveActor(actorId, actorLabel), "USER_ROLES_CHANGED",
                "USER", userId, metadata);
        return userDetail(userId);
    }

    @Transactional(readOnly = true)
    public String exportUsers(String query, String status, String role) {
        PagedResponse<AdminUserDirectoryPort.UserRow> rows = adminUserDirectoryPort.searchUsers(
                query == null ? "" : query,
                status == null || status.isBlank() ? null : ma.noba.shared.domain.model.UserStatus.valueOf(status.toUpperCase()),
                role == null ? "" : role,
                0, 5000);
        StringBuilder csv = new StringBuilder("id,first_name,last_name,email,phone,status,roles,created_at\n");
        for (AdminUserDirectoryPort.UserRow row : rows.content()) {
            csv.append(csvRow(row.id(), row.firstName(), row.lastName(), row.email(), row.phone(),
                    row.status(), String.join("|", row.roles()), row.createdAt())).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportTransactions(String status) {
        PagedResponse<AdminPaymentPort.TransactionRow> rows =
                adminPaymentPort.searchTransactions(status == null ? "" : status, 0, 5000);
        StringBuilder csv = new StringBuilder(
                "id,user_id,provider_id,amount,fee,net,currency,status,payment_intent,created_at,completed_at\n");
        for (AdminPaymentPort.TransactionRow row : rows.content()) {
            csv.append(csvRow(row.id(), row.userId(), row.providerId(), row.amount(), row.fee(),
                    row.netAmount(), row.currency(), row.status(), row.paymentIntentId(),
                    row.createdAt(), row.completedAt())).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportPayouts(String status) {
        PagedResponse<AdminPaymentPort.PayoutRow> rows =
                adminPaymentPort.searchPayouts(status == null ? "" : status, 0, 5000);
        StringBuilder csv = new StringBuilder("id,provider_id,amount,currency,status,transfer_id,created_at,paid_at\n");
        for (AdminPaymentPort.PayoutRow row : rows.content()) {
            csv.append(csvRow(row.id(), row.providerId(), row.amount(), row.currency(), row.status(),
                    row.transferId(), row.createdAt(), row.paidAt())).append('\n');
        }
        return csv.toString();
    }

    private static String csvRow(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append(',');
            Object value = values[i];
            if (value == null) continue;
            String text = String.valueOf(value);
            boolean quote = text.contains(",") || text.contains("\"") || text.contains("\n");
            if (quote) {
                row.append('"').append(text.replace("\"", "\"\"")).append('"');
            } else {
                row.append(text);
            }
        }
        return row.toString();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminTransactionView> searchTransactions(String status, int page, int size) {
        PagedResponse<AdminPaymentPort.TransactionRow> rows =
                adminPaymentPort.searchTransactions(status, page, size);
        Map<UUID, String> providerNames = adminProviderPort.names(
                rows.content().stream().map(AdminPaymentPort.TransactionRow::providerId).distinct().toList());
        Map<UUID, String> clientNames = adminUserDirectoryPort.displayNames(
                rows.content().stream().map(AdminPaymentPort.TransactionRow::userId).distinct().toList());
        return PagedResponse.of(
                rows.content().stream().map(row -> new AdminTransactionView(
                        row.id(), row.userId(), clientNames.getOrDefault(row.userId(), "Client"),
                        row.providerId(), providerNames.getOrDefault(row.providerId(), "Provider"),
                        row.amount(), row.fee(), row.netAmount(), row.currency(), row.status(),
                        row.paymentIntentId(), row.failureMessage(), row.createdAt(), row.completedAt()))
                        .toList(),
                rows.page(), rows.size(), rows.totalElements());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminPayoutView> searchPayouts(String status, int page, int size) {
        PagedResponse<AdminPaymentPort.PayoutRow> rows = adminPaymentPort.searchPayouts(status, page, size);
        Map<UUID, String> providerNames = adminProviderPort.names(
                rows.content().stream().map(AdminPaymentPort.PayoutRow::providerId).distinct().toList());
        return PagedResponse.of(
                rows.content().stream().map(row -> new AdminPayoutView(
                        row.id(), row.providerId(), providerNames.getOrDefault(row.providerId(), "Provider"),
                        row.amount(), row.currency(), row.status(), row.transferId(),
                        row.createdAt(), row.paidAt()))
                        .toList(),
                rows.page(), rows.size(), rows.totalElements());
    }

    @Transactional
    public void refundTransaction(UUID actorId, String actorLabel, UUID transactionId) {
        AdminPaymentPort.TransactionRow before = adminPaymentPort.findTransaction(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
        adminPaymentPort.refund(transactionId);
        AdminPaymentPort.TransactionRow after = adminPaymentPort.findTransaction(transactionId).orElse(before);
        Map<String, Object> metadata = statusChange(before.status(), after.status());
        metadata.put("amount", before.amount());
        metadata.put("currency", before.currency());
        metadata.put("providerId", before.providerId().toString());
        auditService.record(actorId, resolveActor(actorId, actorLabel), "PAYMENT_REFUNDED",
                "PAYMENT", transactionId, metadata);
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
