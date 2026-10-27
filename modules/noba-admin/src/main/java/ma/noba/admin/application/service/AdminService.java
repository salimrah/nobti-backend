package ma.noba.admin.application.service;

import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.shared.domain.port.AdminIdentityPort;
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

    public AdminService(AdminIdentityPort adminIdentityPort, AdminQueuePort adminQueuePort,
                        AdminUserPort adminUserPort, AdminProviderPort adminProviderPort) {
        this.adminIdentityPort = adminIdentityPort;
        this.adminQueuePort = adminQueuePort;
        this.adminUserPort = adminUserPort;
        this.adminProviderPort = adminProviderPort;
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
