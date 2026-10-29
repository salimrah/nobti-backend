package ma.noba.app.admin;

import ma.noba.config.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Persists console flag overrides and restores them on boot. */
@Component
public class FeatureFlagStore {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagStore.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final FeatureFlags featureFlags;

    public FeatureFlagStore(NamedParameterJdbcTemplate jdbc, FeatureFlags featureFlags) {
        this.jdbc = jdbc;
        this.featureFlags = featureFlags;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOverrides() {
        try {
            jdbc.query("select name, enabled from feature_flag", rs -> {
                featureFlags.set(rs.getString("name"), rs.getBoolean("enabled"));
            });
        } catch (Exception e) {
            log.debug("No feature-flag overrides loaded: {}", e.getMessage());
        }
    }

    public void save(String feature, boolean enabled) {
        jdbc.update("insert into feature_flag (name, enabled, updated_at) values (:name, :enabled, now()) "
                        + "on conflict (name) do update set enabled = excluded.enabled, updated_at = now()",
                new MapSqlParameterSource()
                        .addValue("name", feature)
                        .addValue("enabled", enabled));
        featureFlags.set(feature, enabled);
    }

    public void saveAll(Map<String, Boolean> flags) {
        flags.forEach((name, enabled) -> {
            if (name != null && enabled != null) {
                save(name.trim(), enabled);
            }
        });
    }
}
