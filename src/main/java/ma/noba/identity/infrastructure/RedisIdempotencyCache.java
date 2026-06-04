package ma.noba.identity.infrastructure;

import ma.noba.shared.IdempotencyCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisIdempotencyCache implements IdempotencyCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisIdempotencyCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String putIfAbsent(String key, String value, int ttlSeconds) {
        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(set) ? value : (String) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrExecute(String key, int ttlSeconds, Supplier<T> supplier) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (T) cached;
        }
        T result = supplier.get();
        redisTemplate.opsForValue().set(key, result, ttlSeconds, TimeUnit.SECONDS);
        return result;
    }
}

