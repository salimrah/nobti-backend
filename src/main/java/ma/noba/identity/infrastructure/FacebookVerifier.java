package ma.noba.identity.infrastructure;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.exception.InvalidTokenException;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialProviderVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FacebookVerifier implements SocialProviderVerifier {

    private final WebClient webClient;

    @Override
    public SocialProfile verify(String token) {
        Map<String, Object> me = webClient.get()
                .uri("https://graph.facebook.com/me?fields=id,name,email&access_token={t}", token)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // consider async in real high-load apps

        if (me == null || me.get("id") == null) {
            throw new InvalidTokenException("Invalid Facebook access token");
        }

        return new SocialProfile(
                (String) me.get("id"),
                (String) me.get("email"),
                (String) me.get("name")
        );
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.FACEBOOK;
    }
}

