package ma.noba;

import ma.noba.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Boots the full application context against the Testcontainers-provided PostgreSQL/Redis.
 * Previously this test hardcoded {@code localhost} datasource/redis URLs (the dev docker-compose
 * services), so it only passed when those happened to be running locally.
 */
class NobaBackendApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
