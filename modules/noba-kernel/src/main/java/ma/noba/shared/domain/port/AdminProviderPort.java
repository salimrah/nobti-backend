package ma.noba.shared.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminProviderPort {
    long countPendingProviders();

    List<PendingProvider> pendingProviders();

    void approveProvider(UUID providerId);

    void rejectProvider(UUID providerId);

    record PendingProvider(UUID id, UUID userId, String name, String category, String status, Instant createdAt) {
    }
}
