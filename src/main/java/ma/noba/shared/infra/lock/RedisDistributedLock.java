package ma.noba.shared.infra.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);
    private static final String LOCK_PREFIX = "lock:";

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String lockName, Duration ttl) {
        String key = LOCK_PREFIX + lockName;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        boolean result = Boolean.TRUE.equals(acquired);
        if (result) {
            log.debug("Acquired distributed lock: {}", lockName);
        } else {
            log.debug("Failed to acquire distributed lock: {} (already held)", lockName);
        }
        return result;
    }

    public void release(String lockName) {
        String key = LOCK_PREFIX + lockName;
        redisTemplate.delete(key);
        log.debug("Released distributed lock: {}", lockName);
    }

    public boolean isLocked(String lockName) {
        String key = LOCK_PREFIX + lockName;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
