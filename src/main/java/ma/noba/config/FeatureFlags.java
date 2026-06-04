package ma.noba.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "features")
public class FeatureFlags {

    private final Map<String, Boolean> flags = new HashMap<>();

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public boolean isEnabled(String feature) {
        return flags.getOrDefault(feature, false);
    }

    public boolean isEnabled(String feature, boolean defaultValue) {
        return flags.getOrDefault(feature, defaultValue);
    }
}
