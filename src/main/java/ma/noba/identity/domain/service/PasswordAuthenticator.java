package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.*;
import ma.noba.identity.application.PasswordAuthRequest;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import ma.noba.shared.DomainService;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.event.EventBus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@DomainService
@RequiredArgsConstructor
public class PasswordAuthenticator implements Authenticator<PasswordAuthRequest, AuthToken> {
    private final UserRepository users;
    private final CredentialPasswordRepository credentialPasswords;
    private final PasswordHasher hasher;
    private final TokenPort tokens;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final HashService hashService;

    @Override
    public boolean supports(AuthMethod method) {
        return method == AuthMethod.PASSWORD;
    }

    @Override @Transactional
    public AuthToken authenticate(PasswordAuthRequest req) {
        String emailNorm = req.email().trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);

        if (!loginAttemptLimiter.isAllowed(emailNorm)) {
            throw new ValidationException(ErrorCode.AUTH_ACCOUNT_LOCKED, "Account is temporarily locked due to too many failed attempts");
        }

        User user = users.findByEmailHash(emailHash)
                .orElseThrow(() -> {
                    loginAttemptLimiter.recordFailure(emailNorm);
                    return new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException(ErrorCode.EMAIL_NOT_VERIFIED, "Email is not verified");
        }

        CredentialPassword credential = credentialPasswords.findByUserId(user.getId())
                .orElseThrow(() -> {
                    loginAttemptLimiter.recordFailure(emailNorm);
                    return new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
                });

        if (!hasher.matches(req.password(), credential.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(emailNorm);
            throw new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
        }

        loginAttemptLimiter.reset(emailNorm);
        auditService.record(user.getId(), "LOGIN", null, null);
        eventBus.publish(new UserAuthenticatedEvent(user.getId().toString(), "email_password"));
        return tokens.issueForUser(user, req.deviceId());
    }
}