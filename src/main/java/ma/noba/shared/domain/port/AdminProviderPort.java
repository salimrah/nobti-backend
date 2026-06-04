package ma.noba.shared.domain.port;

import java.util.UUID;

public interface AdminProviderPort {
    long countPendingProviders();
    void approveProvider(UUID providerId);
    void rejectProvider(UUID providerId);
}
