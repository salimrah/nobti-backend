package ma.noba.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CacheInvalidationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final CacheManager cacheManager;

    public CacheInvalidationListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String cacheName = new String(message.getChannel(), StandardCharsets.UTF_8);
        String key = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if ("*".equals(key)) {
                    cache.clear();
                    log.info("Cache cleared for all entries in: {}", cacheName);
                } else {
                    cache.evict(key);
                    log.debug("Cache evicted for key={} in cache={}", key, cacheName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache: cacheName={}, key={}", cacheName, key, e);
        }
    }

    @org.springframework.context.annotation.Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(this, new ChannelTopic("cache:invalidation:*"));
        return container;
    }
}
