package ma.noba.admin.application.service;

import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import ma.noba.shared.domain.port.AdminProviderPort;
import ma.noba.shared.domain.port.AdminQueuePort;
import ma.noba.shared.domain.port.AdminUserPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminIdentityPort adminIdentityPort;
    private final AdminQueuePort adminQueuePort;
    private final AdminUserPort adminUserPort;
    private final AdminProviderPort adminProviderPort;
    private final AdminUserDirectoryPort adminUserDirectoryPort;

    public AdminService(AdminIdentityPort adminIdentityPort, AdminQueuePort adminQueuePort,
                        AdminUserPort adminUserPort, AdminProviderPort adminProviderPort,
                        AdminUserDirectoryPort adminUserDirectoryPort) {
        this.adminIdentityPort = adminIdentityPort;
        this.adminQueuePort = adminQueuePort;
        this.adminUserPort = adminUserPort;
        this.adminProviderPort = adminProviderPort;
        this.adminUserDirectoryPort = adminUserDirectoryPort;
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
    public void suspendUser(UUID userId) {
        adminIdentityPort.suspendUser(userId);
    }

    @Transactional
    public void activateUser(UUID userId) {
        adminIdentityPort.activateUser(userId);
    }

    @Transactional(readOnly = true)
    public List<AdminProviderPort.PendingProvider> pendingProviders() {
        return adminProviderPort.pendingProviders();
    }

    @Transactional
    public void approveProvider(UUID providerId) {
        adminProviderPort.approveProvider(providerId);
    }

    @Transactional
    public void rejectProvider(UUID providerId) {
        adminProviderPort.rejectProvider(providerId);
    }
}
