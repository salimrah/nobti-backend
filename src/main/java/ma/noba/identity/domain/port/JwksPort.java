package ma.noba.identity.domain.port;

import java.util.Map;

/**
 * Read-only access to the public signing keys for the JWKS endpoint. Implemented by the
 * infrastructure key manager so the controller never depends on crypto internals.
 */
public interface JwksPort {

    Map<String, Object> publicJwks();
}
