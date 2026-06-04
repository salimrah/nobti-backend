package ma.noba.subscription.application.dto;

import ma.noba.subscription.domain.model.SubscriptionTier;

import java.util.List;

public record TierInfoResponse(
        SubscriptionTier tier,
        String name,
        String description,
        double priceMonthly,
        List<String> features
) {
    public static List<TierInfoResponse> getAllTiers() {
        return List.of(
                new TierInfoResponse(SubscriptionTier.FREE, "Free", "Basic listing to get started", 0,
                        List.of("Basic profile", "1 service", "Standard queue access")),
                new TierInfoResponse(SubscriptionTier.BASIC, "Basic", "Complete profile for growing businesses", 199,
                        List.of("Full profile", "Unlimited services", "Basic analytics", "Business hours")),
                new TierInfoResponse(SubscriptionTier.PREMIUM, "Premium", "Stand out with promotions and priority", 499,
                        List.of("Everything in Basic", "Promotions & discounts", "Badges & verification", "Featured placement", "Priority support")),
                new TierInfoResponse(SubscriptionTier.ENTERPRISE, "Enterprise", "Full platform for chains and clinics", 1499,
                        List.of("Everything in Premium", "Multi-location", "Employee management", "API access", "Dedicated support", "Custom branding"))
        );
    }
}
