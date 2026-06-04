package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.LoginAttemptLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisLoginAttemptLimiter implements LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final String PREFIX = "login:attempts:";

    private final StringRedisTemplate redis;

    public RedisLoginAttemptLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isAllowed(String key) {
        String count = redis.opsForValue().get(PREFIX + key);
        if (count == null) return true;
        return Integer.parseInt(count) < MAX_ATTEMPTS;
    }

    @Override
    public void recordFailure(String key) {
        String redisKey = PREFIX + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redis.expire(redisKey, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
    }

    @Override
    public void reset(String key) {
        redis.delete(PREFIX + key);
    }
}
