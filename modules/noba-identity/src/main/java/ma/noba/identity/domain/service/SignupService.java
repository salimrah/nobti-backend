package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.*;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.VerificationChallengeRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.DomainService;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class SignupService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository users;
    private final CredentialPasswordRepository credentialPasswords;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final VerificationChallengeRepository verificationChallenges;
    private final EmailSender emailSender;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final UserProfileProvisioningPort userProfileProvisioning;
    private final HashService hashService;
    private final UserRolePort userRolePort;

    @Transactional
    public SignupResult signup(SignupData data) {
        passwordPolicy.validate(data.password());

        String emailNorm = data.email().trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);

        if (users.findByEmailHash(emailHash).isPresent()) {
            throw new ValidationException(ErrorCode.EMAIL_TAKEN, "Email is already registered");
        }

        User user = users.save(User.builder()
                .email(emailNorm)
                .emailHash(emailHash)
                .status(UserStatus.PENDING)
                .build());

        String firstName = data.firstName();
        String lastName = data.lastName();
        String displayName = firstName != null
                ? firstName + (lastName != null ? " " + lastName : "")
                : null;
        userProfileProvisioning.createProfile(user.getId(), firstName, lastName, displayName);
        userRolePort.assignRole(user.getId(), "USER");

        credentialPasswords.save(CredentialPassword.builder()
                .user(user)
                .passwordHash(passwordHasher.hash(data.password()))
                .build());

        String verificationCode = generateVerificationCode();
        verificationChallenges.save(VerificationChallenge.builder()
                .user(user)
                .challengeType("email_verify")
                .codeHash(hashService.hash(verificationCode))
                .maxAttempts(5)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build());

        String firstNameForEmail = data.firstName() != null ? data.firstName() : "User";
        sendVerificationEmail(emailNorm, firstNameForEmail, verificationCode);

        auditService.record(user.getId(), "SIGNUP", null, null);
        eventBus.publish(new UserSignedUpEvent(user.getId().toString(), emailNorm, user.getPhone(), user.getStatus(), user.getOnboardingStatus()));

        return new SignupResult(user.getId(), emailNorm, false);
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);

        User user = users.findByEmailHash(emailHash)
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_ERROR, "No user found for this email"));

        String codeHash = hashService.hash(code);
        VerificationChallenge challenge = verificationChallenges
                .findByChallengeTypeAndCodeHashAndConsumedAtIsNull("email_verify", codeHash)
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_ERROR, "Invalid or expired verification code"));

        if (challenge.isExpired()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Verification code has expired");
        }

        if (!challenge.getUser().getId().equals(user.getId())) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Verification code does not match this user");
        }

        challenge.consume();
        verificationChallenges.save(challenge);

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);

        auditService.record(user.getId(), "EMAIL_VERIFIED", null, null);
    }

    @Transactional
    public void resendVerification(String email) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);

        User user = users.findByEmailHash(emailHash)
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_ERROR, "No user found for this email"));

        String verificationCode = generateVerificationCode();
        verificationChallenges.save(VerificationChallenge.builder()
                .user(user)
                .challengeType("email_verify")
                .codeHash(hashService.hash(verificationCode))
                .maxAttempts(5)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build());

        sendVerificationEmail(emailNorm, null, verificationCode);
    }

    private String generateVerificationCode() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 32);
    }

    private void sendVerificationEmail(String email, String firstName, String code) {
        String name = firstName != null ? firstName : "User";
        String subject = "Verify your Noba account";
        String body = """
                Hi %s,

                Your verification code is: %s

                This code expires in 24 hours.

                If you didn't create a Noba account, please ignore this email.
                """.formatted(name, code);

        emailSender.send(email, subject, body);
    }

    public record SignupData(String email, String password, String firstName, String lastName) {}
    public record SignupResult(UUID userId, String email, boolean emailVerified) {}
}
