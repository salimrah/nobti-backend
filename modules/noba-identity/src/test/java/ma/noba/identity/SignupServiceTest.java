package ma.noba.identity;

import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.CredentialPassword;
import ma.noba.identity.domain.model.VerificationChallenge;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.VerificationChallengeRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.*;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock UserRepository users;
    @Mock CredentialPasswordRepository credentialPasswords;
    @Mock PasswordHasher passwordHasher;
    @Mock PasswordPolicy passwordPolicy;
    @Mock VerificationChallengeRepository verificationChallenges;
    @Mock EmailSender emailSender;
    @Mock AuditService auditService;
    @Mock EventBus eventBus;
    @Mock UserProfileProvisioningPort userProfileProvisioning;
    @Mock HashService hashService;
    @Mock UserRolePort userRolePort;

    private SignupService service;

    @BeforeEach
    void setUp() {
        service = new SignupService(users, credentialPasswords, passwordHasher, passwordPolicy,
                verificationChallenges, emailSender, auditService, eventBus,
                userProfileProvisioning, hashService, userRolePort);
    }

    @Test
    @DisplayName("Signup success — creates user, profile, credential, publishes event")
    void signup_success() {
        var data = new SignupService.SignupData("test@example.com", "Str0ng!Pass", "John", "Doe");
        var emailHash = "email-hash";
        var user = User.builder().id(UUID.randomUUID()).email("test@example.com").emailHash(emailHash).status(UserStatus.PENDING).build();
        when(hashService.hash("test@example.com")).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.empty());
        when(users.save(any())).thenReturn(user);
        when(passwordHasher.hash(any())).thenReturn("hashed-pw");

        var result = service.signup(data);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.userId()).isEqualTo(user.getId());
        verify(passwordPolicy).validate("Str0ng!Pass");
        verify(userProfileProvisioning).createProfile(user.getId(), "John", "Doe", "John Doe");
        verify(userRolePort).assignRole(user.getId(), "USER");
        verify(credentialPasswords).save(any(CredentialPassword.class));
        verify(verificationChallenges).save(any(VerificationChallenge.class));
        verify(emailSender).send(eq("test@example.com"), anyString(), anyString());
        verify(eventBus).publish(any(UserSignedUpEvent.class));
    }

    @Test
    @DisplayName("Signup with taken email — throws EMAIL_TAKEN")
    void signup_emailTaken() {
        var data = new SignupService.SignupData("taken@example.com", "Str0ng!Pass", null, null);
        var emailHash = "taken-hash";
        when(hashService.hash("taken@example.com")).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> service.signup(data))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.EMAIL_TAKEN);

        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("Signup with null names — creates profile with null displayName")
    void signup_nullNames() {
        var data = new SignupService.SignupData("noname@example.com", "Str0ng!Pass", null, null);
        var emailHash = "noname-hash";
        var user = User.builder().id(UUID.randomUUID()).email("noname@example.com").emailHash(emailHash).status(UserStatus.PENDING).build();
        when(hashService.hash("noname@example.com")).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.empty());
        when(users.save(any())).thenReturn(user);
        when(passwordHasher.hash(any())).thenReturn("hashed-pw");

        service.signup(data);

        verify(userProfileProvisioning).createProfile(user.getId(), null, null, null);
        verify(userRolePort).assignRole(user.getId(), "USER");
    }

    @Test
    @DisplayName("Verify email success — consumes challenge, marks email verified")
    void verifyEmail_success() {
        var email = "test@example.com";
        var emailHash = "hash";
        var code = "valid-code";
        var codeHash = "code-hash";
        var user = User.builder().id(UUID.randomUUID()).email(email).emailHash(emailHash).build();
        var challenge = VerificationChallenge.builder()
                .user(user)
                .challengeType("email_verify")
                .codeHash(codeHash)
                .maxAttempts(5)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        when(hashService.hash(email)).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.of(user));
        when(hashService.hash(code)).thenReturn(codeHash);
        when(verificationChallenges.findByChallengeTypeAndCodeHashAndConsumedAtIsNull("email_verify", codeHash))
                .thenReturn(Optional.of(challenge));

        service.verifyEmail(email, code);

        assertThat(challenge.isConsumed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Verify email expired code — throws VALIDATION_ERROR")
    void verifyEmail_expired() {
        var email = "test@example.com";
        var code = "expired-code";
        var codeHash = "expired-hash";
        var user = User.builder().id(UUID.randomUUID()).email(email).build();
        var challenge = VerificationChallenge.builder()
                .user(user)
                .codeHash(codeHash)
                .maxAttempts(5)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(hashService.hash(email)).thenReturn("email-hash");
        when(users.findByEmailHash("email-hash")).thenReturn(Optional.of(user));
        when(hashService.hash(code)).thenReturn(codeHash);
        when(verificationChallenges.findByChallengeTypeAndCodeHashAndConsumedAtIsNull("email_verify", codeHash))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.verifyEmail(email, code))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Resend verification — creates new challenge, sends email")
    void resendVerification_success() {
        var email = "test@example.com";
        var emailHash = "hash";
        var user = User.builder().id(UUID.randomUUID()).email(email).emailHash(emailHash).build();
        when(hashService.hash(email)).thenReturn(emailHash);
        when(users.findByEmailHash(emailHash)).thenReturn(Optional.of(user));

        service.resendVerification(email);

        verify(verificationChallenges).save(any(VerificationChallenge.class));
        verify(emailSender).send(eq(email), anyString(), anyString());
    }
}
