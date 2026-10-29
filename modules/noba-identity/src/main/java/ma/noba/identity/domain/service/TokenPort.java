package ma.noba.identity.domain.service;

import ma.noba.identity.application.AuthToken;
import ma.noba.identity.application.dto.ActiveSessionView;
import ma.noba.identity.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Token/session use cases consumed by the auth API. Implemented by the infrastructure token
 * service; controllers depend on this contract, never on the adapter.
 */
public interface TokenPort {

    AuthToken issueForUser(User user, UUID deviceId);

    AuthToken refresh(String refreshToken);

    List<ActiveSessionView> getActiveSessions(UUID userId);

    void revoke(String accessToken);

    void revokeRefreshToken(String tokenHash);

    void revokeAllUserTokens(UUID userId);

    boolean revokeSession(UUID tokenId, UUID userId);

    /** Short-lived token proving the password step succeeded (TOTP still pending). */
    String issueMfaToken(User user);

    /** @return the user id when the token is a valid, unexpired MFA token. */
    Optional<UUID> verifyMfaToken(String token);
}
