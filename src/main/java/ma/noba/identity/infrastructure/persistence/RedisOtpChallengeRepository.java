package ma.noba.identity.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.OtpChallenge;
import ma.noba.identity.domain.repository.OtpChallengeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisOtpChallengeRepository implements OtpChallengeRepository {

    private final RedisTemplate<String, Object> redis;

    private String key(String id){ return "otp:challenge:" + id; }

    @Override
    public void save(OtpChallenge challenge, Duration ttl) {
        redis.opsForValue().set(key(challenge.getId()), challenge, ttl);
    }

    @Override
    public Optional<OtpChallenge> find(String id) {
        Object o = redis.opsForValue().get(key(id));
        if (o instanceof OtpChallenge challenge) return Optional.of(challenge);
        return Optional.empty();
    }

    @Override
    public boolean delete(String id) {
        return Boolean.TRUE.equals(redis.delete(key(id)));
    }
}
