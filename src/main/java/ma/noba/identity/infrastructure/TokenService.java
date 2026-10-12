package ma.noba.identity.infrastructure;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.application.AuthToken;
import ma.noba.identity.domain.model.RefreshToken;
import ma.noba.identity.domain.model.User;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.identity.domain.repository.RefreshTokenRepository;
import ma.noba.identity.domain.port.RevokedTokenStore;
import ma.noba.identity.domain.service.TokenPort;
import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService implements TokenPort {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtTokenProperties props;
    private final RefreshTokenRepository refreshRepo;
    private final JwtKeyManager keyManager;
    private final RevokedTokenStore revokedTokenStore;

    @Transactional
    public AuthToken issueForUser(User user, UUID deviceId) {
        int activeCount = refreshRepo.countByUserIdAndRevokedAtIsNull(user.getId());
        int maxSessions = props.jwt.maxActiveSessions;
        if (activeCount >= maxSessions) {
            List<RefreshToken> activeTokens = refreshRepo.findByUserIdAndRevokedAtIsNull(user.getId());
            activeTokens.sort(Comparator.comparing(RefreshToken::getLastUsedAt));
            int toEvict = activeCount - maxSessions + 1;
            for (int i = 0; i < toEvict && i < activeTokens.size(); i++) {
                RefreshToken oldest = activeTokens.get(i);
                oldest.revoke("evicted: max sessions exceeded");
                refreshRepo.save(oldest);
                log.info("Evicted oldest session {} for user {} (max {} active)", oldest.getId(), user.getId(), maxSessions);
            }
        }

        String access = signAccessToken(user);
        String refresh = secureRandomUrlSafe(64);
        var rt = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(refresh))
                .deviceId(deviceId)
                .expiresAt(Instant.now().plusSeconds(props.jwt.refreshTokenTtlDays * 86400L))
                .build();
        refreshRepo.save(rt);
        boolean isNewUser = user.getOnboardingStatus() == OnboardingStatus.NEW;
        return new AuthToken(access, refresh, props.jwt.accessTokenTtlSeconds, isNewUser);
    }

    @Transactional
    public AuthToken refresh(String refreshToken) {
        String hash = sha256(refreshToken);
        // Look up the token regardless of revocation so a previously rotated/revoked token can be
        // recognised as reuse instead of being silently reported as merely invalid.
        var record = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (record.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}, revoking all sessions", record.getUser().getId());
            revokeAllUserTokens(record.getUser().getId());
            throw new ValidationException(ErrorCode.AUTH_TOKEN_REUSE, "Refresh token reuse detected");
        }
        if (record.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Expired refresh token");

        UUID deviceId = record.getDeviceId();
        record.revoke("rotated");
        refreshRepo.save(record);

        var user = record.getUser();
        return issueForUser(user, deviceId);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshRepo.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(rt -> {
                    rt.revoke("revoke_all");
                    refreshRepo.save(rt);
                });
    }

    @Transactional
    public boolean revokeSession(UUID tokenId, UUID userId) {
        return refreshRepo.revokeByIdAndUserId(tokenId, userId) > 0;
    }

    @Transactional(readOnly = true)
    public List<ActiveSession> getActiveSessions(UUID userId) {
        return refreshRepo.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(rt -> new ActiveSession(
                        rt.getId(),
                        rt.getCreatedAt(),
                        rt.getExpiresAt(),
                        rt.getLastUsedAt()))
                .toList();
    }

    public record ActiveSession(UUID id, Instant createdAt, Instant expiresAt, Instant lastUsedAt) {}

    private String signAccessToken(User user) {
        try {
            var jti = UUID.randomUUID().toString();
            PrivateKey pk = keyManager.currentPrivateKey();
            JWSSigner signer = new RSASSASigner(pk);
            var now = Instant.now();
            var claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .jwtID(jti)
                    .issuer(props.jwt.issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(props.jwt.accessTokenTtlSeconds)))
                    .claim("scope", "user")
                    .claim("onboarding_status", user.getOnboardingStatus() != null ? user.getOnboardingStatus().name() : "NEW")
                    .build();
            var jwt = new SignedJWT(keyManager.currentHeader(), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void revoke(String accessToken) {
        try {
            var signedJWT = SignedJWT.parse(accessToken);
            var jti = signedJWT.getJWTClaimsSet().getJWTID();
            var exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (jti != null && exp != null) {
                long ttl = exp.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
                if (ttl > 0) {
                    revokedTokenStore.revoke(jti, java.time.Duration.ofSeconds(ttl));
                }
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid access token", e);
        }
    }

    @Transactional
    public void revokeRefreshToken(String tokenHash) {
        refreshRepo.findByTokenHash(tokenHash)
                .filter(rt -> !rt.isRevoked())
                .ifPresent(rt -> {
                    rt.revoke("manual_revoke");
                    refreshRepo.save(rt);
                });
    }

    private static String secureRandomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        new java.security.SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
    private static String sha256(String s){
        return DigestUtils.sha256Hex(s);
    }
}