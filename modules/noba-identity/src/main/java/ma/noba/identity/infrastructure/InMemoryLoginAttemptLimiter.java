package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.LoginAttemptLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 15 * 60 * 1000L;

    private final Map<String, Attempts> store = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key) {
        Attempts a = store.get(key);
        if (a == null) return true;
        if (System.currentTimeMillis() - a.start > LOCKOUT_MS) {
            store.remove(key);
            return true;
        }
        return a.count < MAX_ATTEMPTS;
    }

    @Override
    public void recordFailure(String key) {
        store.compute(key, (k, a) -> {
            long now = System.currentTimeMillis();
            if (a == null || now - a.start > LOCKOUT_MS) {
                return new Attempts(now, 1);
            }
            return new Attempts(a.start, a.count + 1);
        });
    }

    @Override
    public void reset(String key) {
        store.remove(key);
    }

    private record Attempts(long start, int count) {}
}
