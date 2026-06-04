package ma.noba.shared.infra.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class ApiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimiter.class);
    private static final String PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public ApiRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        try {
            String redisKey = PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }

            boolean allowed = count != null && count <= maxRequests;
            if (!allowed) {
                log.debug("Rate limit exceeded for key: {} ({} > {})", key, count, maxRequests);
            }
            return allowed;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for rate limiting — allowing request (key={}). " +
                    "Rate limiting is temporarily disabled.", key);
            return true;
        } catch (Exception e) {
            log.error("Unexpected error in rate limiter (key={}) — allowing request: {}", key, e.getMessage());
            return true;
        }
    }

    public long getRemainingRequests(String key, int maxRequests) {
        try {
            String redisKey = PREFIX + key;
            String countStr = redisTemplate.opsForValue().get(redisKey);
            if (countStr == null) {
                return maxRequests;
            }
            long count = Long.parseLong(countStr);
            return Math.max(0, maxRequests - count);
        } catch (RedisConnectionFailureException e) {
            return maxRequests;
        } catch (Exception e) {
            return maxRequests;
        }
    }

    public long getResetTimeSeconds(String key) {
        try {
            String redisKey = PREFIX + key;
            Long ttl = redisTemplate.getExpire(redisKey);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (RedisConnectionFailureException e) {
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
