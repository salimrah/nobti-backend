package ma.noba.identity.infrastructure;

import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.noba.identity.domain.port.RevokedTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;

@Component
public class JwtLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtLogoutHandler.class);
    private final RevokedTokenStore revokedTokenStore;

    public JwtLogoutHandler(RevokedTokenStore revokedTokenStore) {
        this.revokedTokenStore = revokedTokenStore;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String jti = null;
        Long ttlSeconds = null;

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            jti = jwtAuth.getToken().getId();
            ttlSeconds = jwtAuth.getToken().getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        } else {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    var signedJWT = SignedJWT.parse(header.substring(7));
                    jti = signedJWT.getJWTClaimsSet().getJWTID();
                    var exp = signedJWT.getJWTClaimsSet().getExpirationTime();
                    if (exp != null) {
                        ttlSeconds = exp.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
                    }
                } catch (ParseException e) {
                    log.warn("Failed to parse JWT in logout handler", e);
                }
            }
        }

        if (jti != null && ttlSeconds != null && ttlSeconds > 0) {
            revokedTokenStore.revoke(jti, Duration.ofSeconds(ttlSeconds));
        }
    }
}
