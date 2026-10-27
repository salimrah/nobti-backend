package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.port.RevokedTokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRevokedTokenStore implements RevokedTokenStore {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Duration ttl) {
        revoked.put(jti, Instant.now().plus(ttl));
    }

    @Override
    public boolean isRevoked(String jti) {
        Instant expiry = revoked.get(jti);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}
