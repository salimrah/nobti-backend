package ma.noba.badge.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.badge.application.dto.BadgeResponse;
import ma.noba.badge.domain.model.BadgeType;
import ma.noba.badge.domain.model.ProviderBadge;
import ma.noba.badge.domain.port.ProviderBadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BadgeApplicationService {

    private final ProviderBadgeRepository badgeRepository;

    public List<BadgeResponse> getProviderBadges(UUID providerId) {
        return badgeRepository.findByProviderId(providerId).stream()
            .map(BadgeApplicationService::toResponse)
            .toList();
    }

    public List<BadgeResponse> getBadgesForProviders(List<UUID> providerIds) {
        return badgeRepository.findByProviderIdIn(providerIds).stream()
            .map(BadgeApplicationService::toResponse)
            .toList();
    }

    @Transactional
    public BadgeResponse awardBadge(UUID providerId, String badgeTypeStr, boolean autoAwarded) {
        BadgeType badgeType = BadgeType.valueOf(badgeTypeStr.toUpperCase());

        if (badgeRepository.findByProviderIdAndBadgeType(providerId, badgeType).isPresent()) {
            throw new IllegalArgumentException("Provider already has this badge: " + badgeType);
        }

        ProviderBadge badge = new ProviderBadge(providerId, badgeType, autoAwarded);
        badge = badgeRepository.save(badge);
        return toResponse(badge);
    }

    @Transactional
    public void removeBadge(UUID badgeId) {
        ProviderBadge badge = badgeRepository.findById(badgeId)
            .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));
        badgeRepository.delete(badge);
    }

    @Transactional
    public void autoAwardBadges() {
        // Auto-award logic is a scheduled job stub
        // TOP_RATED: 4.5+ avg, 50+ reviews
        // QUICK_SERVICE: avg wait < 10min
        // Implementation would query rating/wait-time aggregates
    }

    public static BadgeResponse toResponse(ProviderBadge badge) {
        return new BadgeResponse(
            badge.getId().toString(),
            badge.getProviderId().toString(),
            badge.getBadgeType().name(),
            badge.getAwardedAt(),
            badge.getExpiresAt(),
            badge.isAutoAwarded()
        );
    }
}
