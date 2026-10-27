package ma.noba.identity.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oidc")
public class OidcProperties {

    private Map<String, Provider> providers = Collections.emptyMap();

    @Getter
    @Setter
    public static class Provider {
        private String issuer;
        private String jwksUrl;
        private String clientId;
    }
}
