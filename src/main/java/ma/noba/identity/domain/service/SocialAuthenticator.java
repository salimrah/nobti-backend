package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.application.AuthToken;
import ma.noba.identity.domain.model.*;
import ma.noba.identity.application.SocialAuthRequest;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.UserIdentityRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import ma.noba.shared.DomainService;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.model.UserStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class SocialAuthenticator implements Authenticator<SocialAuthRequest, AuthToken> {

    private final SocialVerifierService socialVerifierService;
    private final UserIdentityRepository identities;
    private final UserRepository users;
    private final TokenPort tokens;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final UserProfileProvisioningPort userProfileProvisioning;
    private final HashService hashService;
    private final UserRolePort userRolePort;

    @Override public boolean supports(AuthMethod method) { return method == AuthMethod.SOCIAL; }

    @Override @Transactional
    public AuthToken authenticate(SocialAuthRequest req) {
        var profile = socialVerifierService.verify(req.provider(), req.idToken());
        var subHash = socialIdentifierHash(req.provider().name(), profile.subject());
        var existing = identities.findByProviderTypeAndIdentifierHash(
                req.provider().name().toLowerCase(Locale.ROOT), subHash);
        User user = existing.map(UserIdentity::getUser).orElseGet(() -> {
            String email = profile.email();
            var userByEmail = email != null
                    ? users.findByEmailHash(hashService.hash(email.toLowerCase(Locale.ROOT)))
                    : Optional.<User>empty();
            var u = userByEmail.orElseGet(() -> {
                String emailHash = email != null ? hashService.hash(email.toLowerCase(Locale.ROOT)) : null;
                var newUser = users.save(User.builder()
                        .id(UUID.randomUUID())
                        .email(email)
                        .emailHash(emailHash)
                        .emailVerified(email != null)
                        .status(UserStatus.ACTIVE)
                        .build());
                userProfileProvisioning.createProfile(newUser.getId(), null, null, profile.name());
                userRolePort.assignRole(newUser.getId(), "USER");
                eventBus.publish(new UserSignedUpEvent(
                        newUser.getId().toString(),
                        email,
                        null,
                        UserStatus.ACTIVE,
                        newUser.getOnboardingStatus()));
                return newUser;
            });
            identities.save(UserIdentity.builder()
                    .user(u)
                    .providerType(req.provider().name().toLowerCase(Locale.ROOT))
                    .identifierHash(subHash)
                    .isPrimary(true)
                    .verifiedAt(Instant.now())
                    .build());
            return u;
        });
        auditService.record(user.getId(), "SOCIAL_LOGIN", null, null);
        eventBus.publish(new UserAuthenticatedEvent(user.getId().toString(), "oauth:" + req.provider().name().toLowerCase()));
        return tokens.issueForUser(user, req.deviceId());
    }

    private String socialIdentifierHash(String provider, String subject) {
        return hashService.hash(provider.toLowerCase(Locale.ROOT) + ":" + subject);
    }
}
