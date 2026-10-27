package ma.noba.identity;

import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.UserIdentity;
import ma.noba.identity.domain.repository.UserIdentityRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.AccountLinkingService;
import ma.noba.identity.domain.service.AuditService;
import ma.noba.identity.domain.service.HashService;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialVerifierService;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLinkingServiceTest {

    @Mock
    private UserIdentityRepository identities;

    @Mock
    private SocialVerifierService socialVerifierService;

    @Mock
    private UserRepository users;

    @Mock
    private AuditService auditService;

    @Mock
    private HashService hashService;

    private AccountLinkingService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountLinkingService(identities, socialVerifierService, users, auditService, hashService);
        userId = UUID.randomUUID();
        user = User.builder().id(userId).build();
    }

    @Test
    @DisplayName("Should link a new social account")
    void linkAccount_success() {
        var profile = new SocialProfile("ext-123", "ext@test.com", "External User", true);
        when(socialVerifierService.verify(SocialProvider.GOOGLE, "valid-token")).thenReturn(profile);
        when(hashService.hash("google:ext-123")).thenReturn("hash-google-ext-123");
        when(identities.findByProviderTypeAndIdentifierHash("google", "hash-google-ext-123"))
                .thenReturn(Optional.empty());
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(identities.findByUserId(userId)).thenReturn(List.of(
                UserIdentity.builder().user(user).providerType("facebook").isPrimary(true).build()
        ));

        service.linkAccount(userId, SocialProvider.GOOGLE, "valid-token", null);

        verify(identities).save(any(UserIdentity.class));
        verify(auditService).record(userId, "ACCOUNT_LINKED", null, null);
    }

    @Test
    @DisplayName("Should throw when provider is already linked")
    void linkAccount_alreadyLinked() {
        var profile = new SocialProfile("ext-123", "ext@test.com", "External User", true);
        when(socialVerifierService.verify(SocialProvider.GOOGLE, "token")).thenReturn(profile);
        when(hashService.hash("google:ext-123")).thenReturn("hash-google-ext-123");
        when(identities.findByProviderTypeAndIdentifierHash("google", "hash-google-ext-123"))
                .thenReturn(Optional.of(mock(UserIdentity.class)));

        assertThatThrownBy(() -> service.linkAccount(userId, SocialProvider.GOOGLE, "token", null))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTH_ALREADY_LINKED);

        verify(identities, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when user not found")
    void linkAccount_userNotFound() {
        var profile = new SocialProfile("ext-123", "ext@test.com", "External User", true);
        when(socialVerifierService.verify(SocialProvider.GOOGLE, "token")).thenReturn(profile);
        when(hashService.hash("google:ext-123")).thenReturn("hash-google-ext-123");
        when(identities.findByProviderTypeAndIdentifierHash("google", "hash-google-ext-123"))
                .thenReturn(Optional.empty());
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.linkAccount(userId, SocialProvider.GOOGLE, "token", null))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Should unlink a provider when multiple accounts exist")
    void unlinkAccount_success() {
        var facebookIdentity = UserIdentity.builder().user(user).providerType("facebook").isPrimary(true).build();
        var googleIdentity = UserIdentity.builder().user(user).providerType("google").isPrimary(true).build();
        when(identities.findByUserId(userId)).thenReturn(List.of(facebookIdentity, googleIdentity));

        service.unlinkAccount(userId, SocialProvider.FACEBOOK);

        verify(identities).delete(facebookIdentity);
        verify(auditService).record(userId, "ACCOUNT_UNLINKED", null, null);
    }

    @Test
    @DisplayName("Should throw when unlinking the last provider")
    void unlinkAccount_lastMethod() {
        var googleIdentity = UserIdentity.builder().user(user).providerType("google").isPrimary(true).build();
        when(identities.findByUserId(userId)).thenReturn(List.of(googleIdentity));

        assertThatThrownBy(() -> service.unlinkAccount(userId, SocialProvider.GOOGLE))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.AUTH_LAST_METHOD);
    }

    @Test
    @DisplayName("Should return linked providers")
    void getLinkedProviders() {
        var googleIdentity = UserIdentity.builder().user(user).providerType("google").isPrimary(true).build();
        var appleIdentity = UserIdentity.builder().user(user).providerType("apple").isPrimary(true).build();
        when(identities.findByUserId(userId)).thenReturn(List.of(googleIdentity, appleIdentity));

        List<String> result = service.getLinkedProviders(userId);

        assertThat(result).containsExactlyInAnyOrder("GOOGLE", "APPLE");
    }

    @Test
    @DisplayName("Should return true if provider is linked")
    void hasLinkedAccount_true() {
        var googleIdentity = UserIdentity.builder().user(user).providerType("google").isPrimary(true).build();
        when(identities.findByUserId(userId)).thenReturn(List.of(googleIdentity));

        boolean result = service.hasLinkedAccount(userId, SocialProvider.GOOGLE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false if provider is not linked")
    void hasLinkedAccount_false() {
        when(identities.findByUserId(userId)).thenReturn(List.of());

        boolean result = service.hasLinkedAccount(userId, SocialProvider.GOOGLE);

        assertThat(result).isFalse();
    }
}
