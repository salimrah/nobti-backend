package ma.noba.shared.infra.ratelimit;

public record RateLimitConfig(
        int maxRequests,
        int windowSeconds,
        boolean enabled
) {
    public RateLimitConfig {
        if (maxRequests <= 0) maxRequests = 100;
        if (windowSeconds <= 0) windowSeconds = 60;
    }
}
