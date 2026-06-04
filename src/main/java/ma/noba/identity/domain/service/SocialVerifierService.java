package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.model.TokenType;
import ma.noba.shared.DomainService;

import java.util.List;

@DomainService
@RequiredArgsConstructor
public class SocialVerifierService {

    private final List<SocialProviderVerifier> verifiers;

    public SocialProfile verify(SocialProvider provider, String token) {
        return verifiers.stream()
                .filter(v -> v.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported social provider: " + provider))
                .verify(token);
    }

}
