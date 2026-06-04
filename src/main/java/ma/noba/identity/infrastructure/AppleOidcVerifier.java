package ma.noba.identity.infrastructure;


import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import ma.noba.identity.domain.exception.InvalidTokenException;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialProviderVerifier;
import ma.noba.identity.infrastructure.config.OidcProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@ConditionalOnExpression("'${oauth.apple.client-id:}' != ''")
public class AppleOidcVerifier implements SocialProviderVerifier {

    private final OidcProperties oidcProperties;
    private final DefaultJWTProcessor<SecurityContext> processor;

    public AppleOidcVerifier(OidcProperties oidcProperties, @Qualifier("apple") DefaultJWTProcessor<SecurityContext> processor) {
        this.oidcProperties = oidcProperties;
        this.processor = processor;
    }

    @Override
    public SocialProfile verify(String token) {

        // Get Apple-specific configuration
        OidcProperties.Provider appleConfig = oidcProperties.getProviders().get("apple");

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = processor.process(jwt, null);

            // Validate issuer
            if (!appleConfig.getIssuer().equals(claims.getIssuer())) {
                throw new InvalidTokenException("Invalid issuer");
            }

            // Validate audience
            if (appleConfig.getClientId() != null && !claims.getAudience().contains(appleConfig.getClientId())) {
                throw new InvalidTokenException("Invalid audience");
            }

            // Validate expiration
            if (claims.getExpirationTime().before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            // Apple may use "email" and "sub" for subject
            return new SocialProfile(
                    claims.getSubject(),
                    claims.getStringClaim("email"),
                    claims.getStringClaim("name") // optional, may be null
            );

        } catch (InvalidTokenException e) {
            throw e;
        } catch (BadJWTException e) {
            throw new InvalidTokenException("Token expired", e);
        } catch (Exception e) {
            throw new InvalidTokenException("OIDC verification failed for Apple", e);
        }
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.APPLE;
    }
}
