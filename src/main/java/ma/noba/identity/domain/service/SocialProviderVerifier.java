package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.SocialProvider;

public interface SocialProviderVerifier {
    SocialProfile verify(String token);
    boolean supports(SocialProvider provider);
}
