package ma.noba.identity.infrastructure.adapter;

import ma.noba.identity.domain.model.CredentialPassword;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.HashService;
import ma.noba.identity.domain.service.PasswordHasher;
import ma.noba.identity.domain.service.PasswordPolicy;
import ma.noba.identity.domain.service.TokenPort;
import ma.noba.identity.domain.service.UserFactory;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminIdentityPortAdapter implements AdminIdentityPort {

    private final UserRepository userRepository;
    private final EventBus eventBus;
    private final TokenPort tokenPort;
    private final UserFactory userFactory;
    private final CredentialPasswordRepository credentialPasswords;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final HashService hashService;
    private final UserProfileProvisioningPort profileProvisioning;

    public AdminIdentityPortAdapter(UserRepository userRepository, EventBus eventBus, TokenPort tokenPort,
                                    UserFactory userFactory, CredentialPasswordRepository credentialPasswords,
                                    PasswordHasher passwordHasher, PasswordPolicy passwordPolicy,
                                    HashService hashService, UserProfileProvisioningPort profileProvisioning) {
        this.userRepository = userRepository;
        this.eventBus = eventBus;
        this.tokenPort = tokenPort;
        this.userFactory = userFactory;
        this.credentialPasswords = credentialPasswords;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.hashService = hashService;
        this.profileProvisioning = profileProvisioning;
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    @Override
    public long countSuspendedUsers() {
        return userRepository.countByStatus(UserStatus.DISABLED);
    }

    @Override
    public Optional<UserState> findUser(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserState(
                        user.getId(),
                        user.getStatus() != null ? user.getStatus().name() : null,
                        user.getEmail(),
                        user.getPhone(),
                        user.getCreatedAt()));
    }

    @Override
    public UUID createAdmin(String email, String rawPassword, String firstName, String lastName) {
        passwordPolicy.validate(rawPassword);
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        String emailHash = hashService.hash(emailNorm);
        if (userRepository.findByEmailHash(emailHash).isPresent()) {
            throw new ValidationException(ErrorCode.EMAIL_TAKEN, "Email is already registered");
        }
        User user = userFactory.createWithEmail(emailNorm, emailHash);
        credentialPasswords.save(CredentialPassword.builder()
                .user(user)
                .passwordHash(passwordHasher.hash(rawPassword))
                .build());
        if ((firstName != null && !firstName.isBlank()) || (lastName != null && !lastName.isBlank())) {
            String display = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
            profileProvisioning.createProfile(user.getId(), firstName, lastName, display);
        }
        return user.getId();
    }

    @Override
    public List<SessionRow> sessions(UUID userId) {
        return tokenPort.getActiveSessions(userId).stream()
                .map(session -> new SessionRow(
                        session.id(), session.createdAt(), session.expiresAt(), session.lastUsedAt()))
                .toList();
    }

    @Override
    public void revokeSessions(UUID userId) {
        tokenPort.revokeAllUserTokens(userId);
    }

    @Override
    public void suspendUser(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            var oldStatus = user.getStatus();
            user.setStatus(UserStatus.DISABLED);
            userRepository.save(user);
            // A suspended account must not keep browsing with a still-valid access token.
            tokenPort.revokeAllUserTokens(userId);
            eventBus.publish(new UserStatusChangedEvent(userId, oldStatus, UserStatus.DISABLED));
        });
    }

    @Override
    public void activateUser(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            var oldStatus = user.getStatus();
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            eventBus.publish(new UserStatusChangedEvent(userId, oldStatus, UserStatus.ACTIVE));
        });
    }
}
