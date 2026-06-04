package ma.noba.identity.domain.port;

import java.time.Duration;

public interface RevokedTokenStore {
    void revoke(String jti, Duration ttl);
    boolean isRevoked(String jti);
}
