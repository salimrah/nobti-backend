package ma.noba.loyalty.domain.port.service;

import java.util.UUID;

/** Loyalty configuration configured per provider (with a safe default when unset). */
public interface ProviderLoyaltyPolicyPort {

    /** Points awarded for a completed visit; 0 disables awarding. */
    int pointsPerVisit(UUID providerId);
}
