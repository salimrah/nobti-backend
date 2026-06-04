package ma.noba.badge.domain.port;

import ma.noba.badge.domain.model.BadgeType;
import ma.noba.badge.domain.model.ProviderBadge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderBadgeRepository {
    ProviderBadge save(ProviderBadge badge);
    Optional<ProviderBadge> findById(UUID id);
    List<ProviderBadge> findByProviderId(UUID providerId);
    Optional<ProviderBadge> findByProviderIdAndBadgeType(UUID providerId, BadgeType badgeType);
    List<ProviderBadge> findByBadgeType(BadgeType badgeType);
    List<ProviderBadge> findByProviderIdIn(List<UUID> providerIds);
    void delete(ProviderBadge badge);
}
