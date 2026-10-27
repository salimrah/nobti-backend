package ma.noba.identity;

import ma.noba.identity.domain.model.RefreshToken;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.RevokedTokenStore;
import ma.noba.identity.domain.repository.RefreshTokenRepository;
import ma.noba.identity.infrastructure.TokenService;
import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock RefreshTokenRepository refreshRepo;
    @Mock RevokedTokenStore revokedTokenStore;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtTokenProperties props = new JwtTokenProperties();
        props.jwt.issuer = "https://api.noba.ma";
        props.jwt.accessTokenTtlSeconds = 3600;
        props.jwt.refreshTokenTtlDays = 7;
        props.jwt.maxActiveSessions = 10;
        props.jwt.idleTimeoutDays = 30;
        tokenService = new TokenService(props, refreshRepo, new JwtKeyManager(props), revokedTokenStore);
    }

    private RefreshToken activeToken(User user, String raw) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(DigestUtils.sha256Hex(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private User activeUser() {
        return User.builder().id(UUID.randomUUID()).status(UserStatus.ACTIVE)
                .onboardingStatus(OnboardingStatus.NEW).build();
    }

    @Test
    @DisplayName("Refresh rotates the token and returns a brand new pair")
    void refreshRotatesToken() {
        User user = activeUser();
        String raw = "raw-refresh-token";
        RefreshToken record = activeToken(user, raw);
        when(refreshRepo.findByTokenHash(DigestUtils.sha256Hex(raw))).thenReturn(Optional.of(record));
        lenient().when(refreshRepo.countByUserIdAndRevokedAtIsNull(user.getId())).thenReturn(0);
        lenient().when(refreshRepo.save(any())).thenReturn(null);

        var tokens = tokenService.refresh(raw);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotEqualTo(raw);
        assertThat(record.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Refreshing an already-rotated (revoked) token is treated as reuse and revokes all sessions")
    void reuseOfRotatedTokenIsDetected() {
        User user = activeUser();
        String raw = "raw-refresh-token";
        RefreshToken record = activeToken(user, raw);
        record.revoke("rotated");
        when(refreshRepo.findByTokenHash(DigestUtils.sha256Hex(raw))).thenReturn(Optional.of(record));
        when(refreshRepo.findByUserIdAndRevokedAtIsNull(user.getId())).thenReturn(List.of(record));
        lenient().when(refreshRepo.save(any())).thenReturn(null);

        assertThatThrownBy(() -> tokenService.refresh(raw))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.AUTH_TOKEN_REUSE);

        verify(refreshRepo).findByUserIdAndRevokedAtIsNull(user.getId());
    }

    @Test
    @DisplayName("Unknown refresh token is rejected")
    void unknownTokenIsRejected() {
        when(refreshRepo.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refresh("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
