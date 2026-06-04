package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisRateLimiterService implements RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Boolean> rateLimitScript;

    @Value("${otp.rate.limit.max-requests:5}")
    private long maxRequests;

    @Value("${otp.rate.limit.window-minutes:1}")
    private long windowMinutes;

    public RedisRateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(
                "local key = KEYS[1]\n" +
                "local tokens = tonumber(ARGV[1])\n" +
                "local max = tonumber(ARGV[2])\n" +
                "local ttl = tonumber(ARGV[3])\n" +
                "local current = redis.call('INCRBY', key, tokens)\n" +
                "redis.call('EXPIRE', key, ttl)\n" +
                "return current <= max\n"
        );
        this.rateLimitScript.setResultType(Boolean.class);
    }

    @Override
    public boolean tryConsume(String key, long tokens) {
        String redisKey = "rate_limit:" + key;
        return Boolean.TRUE.equals(redisTemplate.execute(
                rateLimitScript,
                List.of(redisKey),
                String.valueOf(tokens),
                String.valueOf(maxRequests),
                String.valueOf(windowMinutes * 60)
        ));
    }
}