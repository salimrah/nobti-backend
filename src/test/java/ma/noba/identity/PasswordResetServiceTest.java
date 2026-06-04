package ma.noba.identity;

import ma.noba.identity.domain.model.CredentialPassword;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.VerificationChallenge;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.VerificationChallengeRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.*;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository users;
    @Mock CredentialPasswordRepository credentialPasswords;
    @Mock VerificationChallengeRepository verificationChallenges;
    @Mock PasswordHasher passwordHasher;
    @Mock PasswordPolicy passwordPolicy;
    @Mock EmailSender emailSender;
    @Mock AuditService auditService;
    @Mock HashService hashService;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(users, credentialPasswords, verificationChallenges,
                passwordHasher, passwordPolicy, emailSender, auditService, hashService);
    }

    @Test
    @DisplayName("Request reset for existing user — creates challenge, sends email")
    void requestReset_existingUser() {
        var email = "user@example.com";
        var emailHash = "hash";
        var user = User.builder().id(UUID.randomUUID()).email(email).build();
        when(hashService.hash(email)).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.of(user));

        service.requestReset(email);

        verify(verificationChallenges).save(any(VerificationChallenge.class));
        verify(emailSender).send(eq(email), anyString(), anyString());
        verify(auditService).record(user.getId(), "PASSWORD_RESET_REQUESTED", null, null);
    }

    @Test
    @DisplayName("Request reset for unknown email — silently returns")
    void requestReset_unknownEmail() {
        var email = "unknown@example.com";
        when(hashService.hash(email)).thenReturn("unknown-hash");
        when(users.findByEmailHash("unknown-hash")).thenReturn(Optional.empty());

        service.requestReset(email);

        verify(verificationChallenges, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Reset password with valid token — updates password, consumes challenge")
    void resetPassword_validToken() {
        var token = "valid-token";
        var tokenHash = "token-hash";
        var newPassword = "NewStr0ng!Pass";
        var user = User.builder().id(UUID.randomUUID()).build();
        var challenge = VerificationChallenge.builder()
                .user(user)
                .challengeType("password_reset")
                .codeHash(tokenHash)
                .maxAttempts(5)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        var credential = CredentialPassword.builder()
                .user(user)
                .passwordHash("old-hash")
                .build();

        when(hashService.hash(token)).thenReturn(tokenHash);
        when(verificationChallenges.findByChallengeTypeAndCodeHashAndConsumedAtIsNull("password_reset", tokenHash))
                .thenReturn(Optional.of(challenge));
        when(credentialPasswords.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(passwordHasher.hash(newPassword)).thenReturn("new-hash");

        service.resetPassword(token, newPassword);

        verify(passwordPolicy).validate(newPassword);
        verify(passwordHasher).hash(newPassword);
        verify(credentialPasswords).save(argThat(c -> c.getPasswordHash().equals("new-hash")));
        verify(verificationChallenges).save(argThat(c -> c.isConsumed()));
        verify(auditService).record(user.getId(), "PASSWORD_RESET", null, null);
    }

    @Test
    @DisplayName("Reset password with invalid token — throws INVALID_RESET_TOKEN")
    void resetPassword_invalidToken() {
        var token = "invalid-token";
        when(hashService.hash(token)).thenReturn("invalid-hash");
        when(verificationChallenges.findByChallengeTypeAndCodeHashAndConsumedAtIsNull("password_reset", "invalid-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(token, "NewStr0ng!Pass"))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.INVALID_RESET_TOKEN);
    }

    @Test
    @DisplayName("Reset password with expired token — throws INVALID_RESET_TOKEN")
    void resetPassword_expiredToken() {
        var token = "expired-token";
        var tokenHash = "expired-hash";
        var user = User.builder().id(UUID.randomUUID()).build();
        var challenge = VerificationChallenge.builder()
                .user(user)
                .codeHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(hashService.hash(token)).thenReturn(tokenHash);
        when(verificationChallenges.findByChallengeTypeAndCodeHashAndConsumedAtIsNull("password_reset", tokenHash))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.resetPassword(token, "NewStr0ng!Pass"))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.INVALID_RESET_TOKEN);
    }
}
