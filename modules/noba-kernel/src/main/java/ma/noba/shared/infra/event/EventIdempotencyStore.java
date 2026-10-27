package ma.noba.shared.infra.event;

import ma.noba.shared.infra.InstanceIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class EventIdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(EventIdempotencyStore.class);
    private static final String GLOBAL_PREFIX = "event:processed:";
    private static final String INSTANCE_PREFIX = "event:instance:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration INSTANCE_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final String instanceId;

    public EventIdempotencyStore(StringRedisTemplate redisTemplate, InstanceIdProvider instanceIdProvider) {
        this.redisTemplate = redisTemplate;
        this.instanceId = instanceIdProvider.getInstanceId();
    }

    public boolean isProcessed(String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(INSTANCE_PREFIX + instanceId + ":" + eventId));
    }

    public boolean isGloballyProcessed(String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(GLOBAL_PREFIX + eventId));
    }

    public void markProcessed(String eventId) {
        redisTemplate.opsForValue().set(GLOBAL_PREFIX + eventId, "1", TTL);
        redisTemplate.opsForValue().set(INSTANCE_PREFIX + instanceId + ":" + eventId, "1", TTL);
        log.debug("Marked event {} as processed globally", eventId);
    }

    public void markInstanceLocal(String eventId) {
        redisTemplate.opsForValue().set(INSTANCE_PREFIX + instanceId + ":" + eventId, "1", INSTANCE_TTL);
    }
}
