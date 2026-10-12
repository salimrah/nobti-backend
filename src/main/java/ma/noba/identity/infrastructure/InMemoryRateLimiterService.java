package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiterService implements RateLimiterService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${otp.rate.limit.max-requests:5}")
    private long maxRequests;

    @Value("${otp.rate.limit.window-minutes:1}")
    private long windowMinutes;

    @Override
    public boolean tryConsume(String key, long tokens) {
        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60_000;
        // Always record the consumption, then compare. (The previous version kept the old count when
        // over the limit, so `count <= maxRequests` was always true — a no-op limiter.)
        return windows.compute(key, (k, w) -> {
            if (w == null || now - w.start > windowMs) {
                return new Window(now, tokens);
            }
            return new Window(w.start, w.count + tokens);
        }).count <= maxRequests;
    }

    private record Window(long start, long count) {}
}
