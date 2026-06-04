package ma.noba.identity.infrastructure;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.exception.InvalidTokenException;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.domain.model.TokenType;
import ma.noba.identity.domain.service.SocialProfile;
import ma.noba.identity.domain.service.SocialProviderVerifier;
import ma.noba.identity.infrastructure.config.OidcProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@ConditionalOnExpression("'${oauth.google.client-id:}' != ''")
public class GoogleOidcVerifier implements SocialProviderVerifier {

    private final OidcProperties oidcProperties;
    private final DefaultJWTProcessor<SecurityContext> processor;

    public GoogleOidcVerifier(OidcProperties oidcProperties, @Qualifier("google") DefaultJWTProcessor<SecurityContext> processor) {
        this.oidcProperties = oidcProperties;
        this.processor = processor;
    }

    @Override
    public SocialProfile verify(String token) {

        OidcProperties.Provider googleConfig = oidcProperties.getProviders().get("google");

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = processor.process(jwt, null);

            // Validate issuer
            if (!googleConfig.getIssuer().equals(claims.getIssuer())) {
                throw new InvalidTokenException("Invalid issuer");
            }

            // Validate audience
            if (googleConfig.getClientId() != null && !claims.getAudience().contains(googleConfig.getClientId())) {
                throw new InvalidTokenException("Invalid audience");
            }

            // Validate expiration
            if (claims.getExpirationTime().before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            return new SocialProfile(
                    claims.getSubject(),
                    claims.getStringClaim("email"),
                    claims.getStringClaim("name")
            );
        } catch (InvalidTokenException e) {
            throw e;
        } catch (BadJWTException e) {
            throw new InvalidTokenException("Token expired", e);
        } catch (Exception e) {
            throw new InvalidTokenException("OIDC verification failed", e);
        }
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.GOOGLE;
    }
}

