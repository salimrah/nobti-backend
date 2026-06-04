package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.model.UserIdentity;
import ma.noba.identity.domain.repository.UserIdentityRepository;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@org.springframework.stereotype.Service("v2AccountLinkingService")
@RequiredArgsConstructor
public class AccountLinkingService {

    private final UserIdentityRepository identities;
    private final SocialVerifierService socialVerifierService;
    private final UserRepository users;
    private final AuditService auditService;
    private final HashService hashService;

    @Transactional
    public List<String> linkAccount(UUID userId, SocialProvider provider, String idToken, String accessToken) {
        var profile = socialVerifierService.verify(provider, idToken);
        var providerType = provider.name().toLowerCase(Locale.ROOT);
        var subHash = hashService.hash(providerType + ":" + profile.subject());

        if (identities.findByProviderTypeAndIdentifierHash(providerType, subHash).isPresent()) {
            throw new BusinessException(ErrorCode.AUTH_ALREADY_LINKED);
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND, "User not found"));

        identities.save(UserIdentity.builder()
                .user(user)
                .providerType(providerType)
                .identifierHash(subHash)
                .isPrimary(true)
                .verifiedAt(Instant.now())
                .build());

        auditService.record(userId, "ACCOUNT_LINKED", null, null);

        return getLinkedProviders(userId);
    }

    @Transactional
    public List<String> unlinkAccount(UUID userId, SocialProvider provider) {
        List<UserIdentity> userAccounts = identities.findByUserId(userId);

        if (userAccounts.size() <= 1) {
            throw new BusinessException(ErrorCode.AUTH_LAST_METHOD);
        }

        var providerType = provider.name().toLowerCase(Locale.ROOT);
        userAccounts.stream()
                .filter(id -> id.getProviderType().equals(providerType))
                .findFirst()
                .ifPresent(identities::delete);

        auditService.record(userId, "ACCOUNT_UNLINKED", null, null);

        return getLinkedProviders(userId);
    }

    public List<String> getLinkedProviders(UUID userId) {
        return identities.findByUserId(userId).stream()
                .map(id -> id.getProviderType().toUpperCase(Locale.ROOT))
                .toList();
    }

    public boolean hasLinkedAccount(UUID userId, SocialProvider provider) {
        var providerType = provider.name().toLowerCase(Locale.ROOT);
        return identities.findByUserId(userId).stream()
                .anyMatch(id -> id.getProviderType().equals(providerType));
    }
}
