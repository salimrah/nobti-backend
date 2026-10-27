package ma.noba.identity.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class JwtTokenProperties {
    public Jwt jwt = new Jwt();
    public Pii pii = new Pii();

    @Getter @Setter
    public static class Jwt {
        public String issuer;
        public int accessTokenTtlSeconds;
        public int refreshTokenTtlDays;
        public int keyRotationHours;
        public int maxActiveSessions;
        public int idleTimeoutDays;
        /** Optional path to persist signing keys across restarts (dev convenience; use a secret mount in prod). */
        public String keyStorePath;
        /** Optional fixed signing key (PKCS#8 PEM or base64). When set with the public key, rotation/persistence are disabled. */
        public String rsaPrivateKey;
        /** Optional fixed public key (X.509 PEM or base64) matching {@link #rsaPrivateKey}. */
        public String rsaPublicKey;
    }

    @Getter @Setter
    public static class Pii {
        public String secretB64;
    }
}
