package ma.noba.config;

import ma.noba.shared.infra.ratelimit.RateLimitConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private Map<String, RateLimitConfig> limits = new HashMap<>();

    public Map<String, RateLimitConfig> getLimits() {
        if (limits.isEmpty()) {
            limits.put("default", new RateLimitConfig(100, 60, true));
            limits.put("/api/v1/auth", new RateLimitConfig(20, 60, true));
            limits.put("/api/v1/queues", new RateLimitConfig(60, 60, true));
        }
        return limits;
    }

    public void setLimits(Map<String, RateLimitConfig> limits) {
        this.limits = limits;
    }
}
