package ma.noba.identity;

import ma.noba.identity.application.AuthToken;
import ma.noba.identity.application.SocialAuthRequest;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.UserIdentity;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.UserIdentityRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.AuditService;
import ma.noba.identity.domain.service.HashService;
import ma.noba.identity.domain.service.SocialAuthenticator;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialVerifierService;
import ma.noba.identity.domain.service.TokenPort;
import ma.noba.identity.domain.service.UserRolePort;
import ma.noba.shared.domain.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthenticatorTest {

    @Mock SocialVerifierService socialVerifierService;
    @Mock UserIdentityRepository identities;
    @Mock UserRepository users;
    @Mock TokenPort tokens;
    @Mock AuditService auditService;
    @Mock EventBus eventBus;
    @Mock UserProfileProvisioningPort userProfileProvisioning;
    @Mock HashService hashService;
    @Mock UserRolePort userRolePort;

    private SocialAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new SocialAuthenticator(socialVerifierService, identities, users, tokens,
                auditService, eventBus, userProfileProvisioning, hashService, userRolePort);
    }

    @Test
    @DisplayName("An unverified provider email must NOT link to an existing local account")
    void unverifiedEmailDoesNotLinkToExistingAccount() {
        var request = new SocialAuthRequest(SocialProvider.GOOGLE, "id-token", null, null);
        var profile = new SocialProfile("sub-1", "victim@example.com", "Attacker", false);
        when(socialVerifierService.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(profile);
        when(hashService.hash(anyString())).thenReturn("hashed");
        when(identities.findByProviderTypeAndIdentifierHash("google", "hashed")).thenReturn(Optional.empty());
        User created = User.builder().id(UUID.randomUUID()).email("victim@example.com").build();
        when(users.save(any())).thenReturn(created);
        when(tokens.issueForUser(any(), any())).thenReturn(new AuthToken("a", "r", 3600, true));

        authenticator.authenticate(request);

        // Must NOT look up (and therefore must not attach to) an account by the unverified email.
        verify(users, never()).findByEmailHash(anyString());
        verify(users).save(any());
    }

    @Test
    @DisplayName("A verified provider email may link to an existing local account")
    void verifiedEmailLinksToExistingAccount() {
        var request = new SocialAuthRequest(SocialProvider.GOOGLE, "id-token", null, null);
        var profile = new SocialProfile("sub-2", "owner@example.com", "Owner", true);
        when(socialVerifierService.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(profile);
        when(hashService.hash(anyString())).thenReturn("hashed");
        when(identities.findByProviderTypeAndIdentifierHash("google", "hashed")).thenReturn(Optional.empty());
        User existing = User.builder().id(UUID.randomUUID()).email("owner@example.com").build();
        when(users.findByEmailHash(anyString())).thenReturn(Optional.of(existing));
        when(tokens.issueForUser(any(), any())).thenReturn(new AuthToken("a", "r", 3600, false));

        authenticator.authenticate(request);

        verify(users).findByEmailHash(anyString());
        verify(users, never()).save(any());
        verify(identities).save(any(UserIdentity.class));
    }
}
