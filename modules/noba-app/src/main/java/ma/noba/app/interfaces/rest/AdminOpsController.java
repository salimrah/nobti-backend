package ma.noba.app.interfaces.rest;

import ma.noba.config.FeatureFlags;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Runtime facts the console shows on the operations screen. */
@RestController
@RequestMapping("/api/v1/admin/ops")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOpsController {

    private final ObjectProvider<Flyway> flywayProvider;
    private final FeatureFlags featureFlags;

    public AdminOpsController(ObjectProvider<Flyway> flywayProvider, FeatureFlags featureFlags) {
        this.flywayProvider = flywayProvider;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Flyway flyway = flywayProvider.getIfAvailable();
        if (flyway == null) {
            return ResponseEntity.ok(Map.of(
                    "databaseVersion", "unmanaged",
                    "databaseDescription", "Flyway is disabled",
                    "pendingMigrations", List.of(),
                    "uptimeSeconds", Duration.ofMillis(
                            ManagementFactory.getRuntimeMXBean().getUptime()).toSeconds(),
                    "startedAt", Instant.now().minusMillis(
                            ManagementFactory.getRuntimeMXBean().getUptime()).toString(),
                    "javaVersion", System.getProperty("java.version"),
                    "profiles", System.getProperty("spring.profiles.active", "default")));
        }
        MigrationInfo current = flyway.info().current();
        List<String> pending = Arrays.stream(flyway.info().pending())
                .map(MigrationInfo::getVersion)
                .filter(version -> version != null)
                .map(Object::toString)
                .toList();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        return ResponseEntity.ok(Map.of(
                "databaseVersion", current == null ? "none" : current.getVersion().toString(),
                "databaseDescription", current == null ? "" : String.valueOf(current.getDescription()),
                "pendingMigrations", pending,
                "uptimeSeconds", Duration.ofMillis(uptimeMs).toSeconds(),
                "startedAt", Instant.now().minusMillis(uptimeMs).toString(),
                "javaVersion", System.getProperty("java.version"),
                "profiles", String.join(",", System.getProperty("spring.profiles.active", "default"))));
    }

    @GetMapping("/flags")
    public ResponseEntity<Map<String, Boolean>> flags() {
        return ResponseEntity.ok(featureFlags.getFlags());
    }
}
