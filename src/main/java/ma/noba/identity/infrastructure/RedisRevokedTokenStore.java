package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.port.RevokedTokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisRevokedTokenStore implements RevokedTokenStore {

    private final RedisTemplate<String, Object> redis;

    public RedisRevokedTokenStore(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    @Override
    public void revoke(String jti, Duration ttl) {
        redis.opsForValue().set("revoked:" + jti, true, ttl.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey("revoked:" + jti));
    }
}
