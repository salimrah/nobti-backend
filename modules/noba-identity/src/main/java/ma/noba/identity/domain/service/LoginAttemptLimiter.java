package ma.noba.identity.domain.service;

public interface LoginAttemptLimiter {
    boolean isAllowed(String key);
    void recordFailure(String key);
    void reset(String key);
}
