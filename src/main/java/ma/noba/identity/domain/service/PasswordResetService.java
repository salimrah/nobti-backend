package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.CredentialPassword;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.VerificationChallengeRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@org.springframework.stereotype.Service("v2PasswordResetService")
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository users;
    private final CredentialPasswordRepository credentialPasswords;
    private final VerificationChallengeRepository verificationChallenges;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final EmailSender emailSender;
    private final AuditService auditService;
    private final HashService hashService;

    @Transactional
    public void requestReset(String email) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);

        var userOpt = users.findByEmailHash(emailHash);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        String token = generateToken();
        verificationChallenges.save(ma.noba.identity.domain.model.VerificationChallenge.builder()
                .user(user)
                .challengeType("password_reset")
                .codeHash(hashService.hash(token))
                .maxAttempts(5)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        sendResetEmail(emailNorm, token);
        auditService.record(user.getId(), "PASSWORD_RESET_REQUESTED", null, null);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        passwordPolicy.validate(newPassword);

        String tokenHash = hashService.hash(token);
        var challenge = verificationChallenges
                .findByChallengeTypeAndCodeHashAndConsumedAtIsNull("password_reset", tokenHash)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_RESET_TOKEN, "Invalid or expired reset token"));

        if (challenge.isExpired()) {
            throw new ValidationException(ErrorCode.INVALID_RESET_TOKEN, "Reset token has expired or been used");
        }

        credentialPasswords.findByUserId(challenge.getUser().getId())
                .ifPresent(cred -> {
                    cred.setPasswordHash(passwordHasher.hash(newPassword));
                    credentialPasswords.save(cred);
                });

        challenge.consume();
        verificationChallenges.save(challenge);

        auditService.record(challenge.getUser().getId(), "PASSWORD_RESET", null, null);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(String email, String token) {
        String subject = "Reset your Noba password";
        String body = """
                You requested a password reset.

                Use the following code in the app to reset your password:

                %s

                This code expires in 1 hour.

                If you didn't request this, please ignore this email.
                """.formatted(token);

        emailSender.send(email, subject, body);
    }
}
