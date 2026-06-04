package ma.noba.shared.infra.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisIdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyStore.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public CachedResponse get(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }

        try {
            CachedResponseData data = objectMapper.readValue(json, CachedResponseData.class);
            return new CachedResponse(data.statusCode(), data.headers(), Base64.getDecoder().decode(data.body()));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize idempotency cache for key: {}", idempotencyKey, e);
            return null;
        }
    }

    public void save(String idempotencyKey, CachedResponse response) {
        String key = KEY_PREFIX + idempotencyKey;
        try {
            CachedResponseData data = new CachedResponseData(
                    response.statusCode(),
                    response.headers(),
                    Base64.getEncoder().encodeToString(response.body())
            );
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.debug("Cached idempotency response for key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency cache for key: {}", idempotencyKey, e);
        }
    }

    public record CachedResponse(int statusCode, Map<String, String> headers, byte[] body) {}

    private record CachedResponseData(int statusCode, Map<String, String> headers, String body) {}
}
