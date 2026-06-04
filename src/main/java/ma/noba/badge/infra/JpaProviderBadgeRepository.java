package ma.noba.badge.infra;

import ma.noba.badge.domain.model.BadgeType;
import ma.noba.badge.domain.model.ProviderBadge;
import ma.noba.badge.domain.port.ProviderBadgeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProviderBadgeRepository extends JpaRepository<ProviderBadge, UUID>, ProviderBadgeRepository {
    List<ProviderBadge> findByProviderId(UUID providerId);
    Optional<ProviderBadge> findByProviderIdAndBadgeType(UUID providerId, BadgeType badgeType);
    List<ProviderBadge> findByBadgeType(BadgeType badgeType);
    List<ProviderBadge> findByProviderIdIn(List<UUID> providerIds);
}
