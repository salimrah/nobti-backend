package ma.noba.identity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import ma.noba.identity.domain.exception.InvalidTokenException;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialProviderVerifier;
import ma.noba.identity.domain.service.SocialVerifierService;
import ma.noba.identity.infrastructure.AppleOidcVerifier;
import ma.noba.identity.infrastructure.FacebookVerifier;
import ma.noba.identity.infrastructure.GoogleOidcVerifier;
import ma.noba.identity.infrastructure.config.OidcProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialVerifierErrorTest {

    @Mock private WebClient webClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private static DefaultJWTProcessor<SecurityContext> testProcessor;
    private static RSAKey testRSAKey;
    private OidcProperties oidcProperties;

    @BeforeAll
    static void createKeyAndProcessor() throws Exception {
        testRSAKey = new RSAKeyGenerator(2048).generate();
        var keySelector = new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256,
                new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(
                        new com.nimbusds.jose.jwk.JWKSet(testRSAKey)
                )
        );
        testProcessor = new DefaultJWTProcessor<>();
        testProcessor.setJWSKeySelector(keySelector);
    }

    private String signClaims(JWTClaimsSet claims) throws Exception {
        var signer = new RSASSASigner(testRSAKey);
        var signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @BeforeEach
    void setUp() {
        oidcProperties = new OidcProperties();
        var googleConfig = new OidcProperties.Provider();
        googleConfig.setIssuer("https://accounts.google.com");
        googleConfig.setClientId("google-client-id");
        googleConfig.setJwksUrl("https://www.googleapis.com/oauth2/v3/certs");

        var appleConfig = new OidcProperties.Provider();
        appleConfig.setIssuer("https://appleid.apple.com");
        appleConfig.setClientId("apple-client-id");
        appleConfig.setJwksUrl("https://appleid.apple.com/auth/keys");

        Map<String, OidcProperties.Provider> providers = new HashMap<>();
        providers.put("google", googleConfig);
        providers.put("apple", appleConfig);
        oidcProperties.setProviders(providers);
    }

    // ========= SocialVerifierService =========

    @Test
    @DisplayName("SocialVerifierService throws for unsupported provider")
    void socialVerifier_unsupportedProvider() {
        var verifier = new SocialVerifierService(List.of());
        assertThatThrownBy(() -> verifier.verify(SocialProvider.GOOGLE, "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported social provider");
    }

    @Test
    @DisplayName("SocialVerifierService returns profile for supported provider")
    void socialVerifier_supportedProvider() {
        SocialProviderVerifier mockVerifier = org.mockito.Mockito.mock(SocialProviderVerifier.class);
        when(mockVerifier.supports(SocialProvider.GOOGLE)).thenReturn(true);
        when(mockVerifier.verify("valid-token")).thenReturn(
                new SocialProfile("sub-123", "user@test.com", "Test User"));

        var verifier = new SocialVerifierService(List.of(mockVerifier));
        SocialProfile result = verifier.verify(SocialProvider.GOOGLE, "valid-token");

        assertThat(result.subject()).isEqualTo("sub-123");
        assertThat(result.email()).isEqualTo("user@test.com");
    }

    // ========= GoogleOidcVerifier =========

    @Test
    @DisplayName("GoogleOidcVerifier rejects invalid issuer")
    void googleVerifier_invalidIssuer() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://evil.com")
                .audience("google-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().plusSeconds(3600)))
                .build());

        var verifier = new GoogleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid issuer");
    }

    @Test
    @DisplayName("GoogleOidcVerifier rejects invalid audience")
    void googleVerifier_invalidAudience() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience("wrong-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().plusSeconds(3600)))
                .build());

        var verifier = new GoogleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid audience");
    }

    @Test
    @DisplayName("GoogleOidcVerifier rejects expired token")
    void googleVerifier_expiredToken() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience("google-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().minusSeconds(3600)))
                .build());

        var verifier = new GoogleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token expired");
    }

    @Test
    @DisplayName("GoogleOidcVerifier rejects malformed JWT")
    void googleVerifier_malformedToken() {
        var verifier = new GoogleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify("invalid.jwt.token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("OIDC verification failed");
    }

    // ========= AppleOidcVerifier =========

    @Test
    @DisplayName("AppleOidcVerifier rejects invalid issuer")
    void appleVerifier_invalidIssuer() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://evil.com")
                .audience("apple-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().plusSeconds(3600)))
                .build());

        var verifier = new AppleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid issuer");
    }

    @Test
    @DisplayName("AppleOidcVerifier rejects invalid audience")
    void appleVerifier_invalidAudience() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://appleid.apple.com")
                .audience("wrong-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().plusSeconds(3600)))
                .build());

        var verifier = new AppleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid audience");
    }

    @Test
    @DisplayName("AppleOidcVerifier rejects expired token")
    void appleVerifier_expiredToken() throws Exception {
        var token = signClaims(new JWTClaimsSet.Builder()
                .issuer("https://appleid.apple.com")
                .audience("apple-client-id")
                .subject("sub-1")
                .expirationTime(Date.from(java.time.Instant.now().minusSeconds(3600)))
                .build());

        var verifier = new AppleOidcVerifier(oidcProperties, testProcessor);
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token expired");
    }

    // ========= FacebookVerifier =========

    @Test
    @DisplayName("FacebookVerifier rejects null API response")
    void facebookVerifier_nullResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.justOrEmpty(null));

        var verifier = new FacebookVerifier(webClient);
        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid Facebook access token");
    }

    @Test
    @DisplayName("FacebookVerifier rejects response missing id field")
    void facebookVerifier_missingId() {
        Map<String, Object> response = new HashMap<>();
        response.put("email", "test@example.com");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        var verifier = new FacebookVerifier(webClient);
        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid Facebook access token");
    }

    @Test
    @DisplayName("FacebookVerifier accepts valid response")
    void facebookVerifier_success() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "fb-123");
        response.put("email", "user@test.com");
        response.put("name", "Test User");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        var verifier = new FacebookVerifier(webClient);
        SocialProfile profile = verifier.verify("valid-token");

        assertThat(profile.subject()).isEqualTo("fb-123");
        assertThat(profile.email()).isEqualTo("user@test.com");
        assertThat(profile.name()).isEqualTo("Test User");
    }
}
