package ma.noba.identity.infrastructure.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
public class JwtConfig {

    @Bean
    @Qualifier("google")
    @ConditionalOnExpression("'${oauth.google.client-id:}' != ''")
    public DefaultJWTProcessor<SecurityContext> googleJwtProcessor(OidcProperties oidcProperties) throws MalformedURLException {
        return buildProcessor(oidcProperties, "google");
    }

    @Bean
    @Qualifier("apple")
    @ConditionalOnExpression("'${oauth.apple.client-id:}' != ''")
    public DefaultJWTProcessor<SecurityContext> appleJwtProcessor(OidcProperties oidcProperties) throws MalformedURLException {
        return buildProcessor(oidcProperties, "apple");
    }

    private DefaultJWTProcessor<SecurityContext> buildProcessor(OidcProperties oidcProperties, String provider) throws MalformedURLException {
        OidcProperties.Provider config = oidcProperties.getProviders().get(provider);
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(config.getJwksUrl()));
        JWSVerificationKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
        processor.setJWSKeySelector(keySelector);
        return processor;
    }
}
