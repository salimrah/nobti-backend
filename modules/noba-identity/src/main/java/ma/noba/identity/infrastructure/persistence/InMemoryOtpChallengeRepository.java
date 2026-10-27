package ma.noba.identity.infrastructure.persistence;

import ma.noba.identity.domain.model.OtpChallenge;
import ma.noba.identity.domain.repository.OtpChallengeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryOtpChallengeRepository implements OtpChallengeRepository {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    @Override
    public void save(OtpChallenge challenge, Duration ttl) {
        store.put(challenge.getId(), new Entry(challenge, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<OtpChallenge> find(String id) {
        Entry e = store.get(id);
        if (e == null) return Optional.empty();
        if (Instant.now().isAfter(e.expiry)) {
            store.remove(id);
            return Optional.empty();
        }
        return Optional.of(e.challenge);
    }

    @Override
    public boolean delete(String id) {
        attempts.remove(id);
        return store.remove(id) != null;
    }

    @Override
    public int incrementAttempts(String id, Duration ttl) {
        return attempts.merge(id, 1, Integer::sum);
    }

    private record Entry(OtpChallenge challenge, Instant expiry) {}
}
