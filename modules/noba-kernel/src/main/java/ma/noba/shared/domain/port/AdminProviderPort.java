package ma.noba.shared.domain.port;

import ma.noba.shared.application.dto.PagedResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AdminProviderPort {
    long countPendingProviders();

    List<PendingProvider> pendingProviders();

    PagedResponse<ProviderRow> searchProviders(String query, String status, int page, int size);

    Optional<ProviderState> findProvider(UUID providerId);

    /** Provider display name per provider id. */
    Map<UUID, String> names(Collection<UUID> providerIds);

    Optional<ProviderDetail> providerDetail(UUID providerId);

    void approveProvider(UUID providerId);

    void rejectProvider(UUID providerId);

    void suspendProvider(UUID providerId);

    void reactivateProvider(UUID providerId);

    record PendingProvider(UUID id, UUID userId, String name, String category, String status, Instant createdAt) {
    }

    record ProviderRow(UUID id, UUID userId, String name, String category, String status,
                       String address, boolean verified, Instant createdAt) {
    }

    record ProviderState(UUID id, UUID userId, String name, String status) {
    }

    record ProviderDetail(UUID id, UUID userId, String name, String category, String address,
                          String description, String status, boolean verified, Instant createdAt,
                          List<ServiceRow> services) {
    }

    record ServiceRow(UUID id, String name, String category, long durationMinutes,
                      BigDecimal price, boolean active) {
    }
}
