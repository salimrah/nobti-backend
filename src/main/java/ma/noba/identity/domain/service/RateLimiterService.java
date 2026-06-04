package ma.noba.identity.domain.service;

public interface RateLimiterService {
    boolean tryConsume(String key, long tokens);
}
