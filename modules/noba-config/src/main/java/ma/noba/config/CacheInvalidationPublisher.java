package ma.noba.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class CacheInvalidationPublisher {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public CacheInvalidationPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evict(String cacheName, String key) {
        try {
            String channel = "cache:invalidation:" + cacheName;
            redisTemplate.convertAndSend(channel, key);
            log.debug("Published cache eviction: cache={}, key={}", cacheName, key);
        } catch (Exception e) {
            log.warn("Failed to publish cache eviction: cache={}, key={}", cacheName, key, e);
        }
    }

    public void evictAll(String cacheName) {
        try {
            String channel = "cache:invalidation:" + cacheName;
            redisTemplate.convertAndSend(channel, "*");
            log.info("Published cache clear: cache={}", cacheName);
        } catch (Exception e) {
            log.warn("Failed to publish cache clear: cache={}", cacheName, e);
        }
    }
}
