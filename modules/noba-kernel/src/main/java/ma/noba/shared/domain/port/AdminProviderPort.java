package ma.noba.shared.domain.port;

import ma.noba.shared.application.dto.PagedResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminProviderPort {
    long countPendingProviders();

    List<PendingProvider> pendingProviders();

    PagedResponse<ProviderRow> searchProviders(String query, String status, int page, int size);

    void approveProvider(UUID providerId);

    void rejectProvider(UUID providerId);

    record PendingProvider(UUID id, UUID userId, String name, String category, String status, Instant createdAt) {
    }

    record ProviderRow(UUID id, UUID userId, String name, String category, String status,
                       String address, boolean verified, Instant createdAt) {
    }
}
