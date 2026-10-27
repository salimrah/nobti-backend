package ma.noba.shared;

import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.domain.model.UserStatus;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

/**
 * Base class for integration tests.
 *
 * <p>Uses the Testcontainers <em>singleton container</em> pattern: the containers are started once
 * in a static initializer and never stopped by a per-class lifecycle. This matters because Spring
 * caches the application context across test classes; with the previous {@code @Testcontainers}
 * per-class lifecycle the containers were stopped after the first class while cached contexts
 * (Redis listeners, schedulers) kept running against the dead containers, causing Redis command
 * timeouts and "connection refused" errors throughout the rest of the suite.
 *
 * <p>The {@code test} profile is activated so Hibernate builds the schema with
 * {@code ddl-auto=create-drop} (Flyway is disabled for tests).
 */
@Tag("docker")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("noba_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7").withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // spring.data.redis.url takes precedence over host/port, so set all three consistently.
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.url",
                () -> "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
        registry.add("spring.flyway.enabled", () -> false);
        // Use the in-memory "log" senders; "none" leaves no SmsGateway/WhatsAppGateway bean and
        // the OTP service fails to wire.
        registry.add("otp.providers.sms.active", () -> "log");
        registry.add("otp.providers.whatsapp.active", () -> "log");
        registry.add("security.pii.secret-b64", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("security.jwt.access-token-ttl-seconds", () -> "3600");
        registry.add("security.jwt.refresh-token-ttl-days", () -> "7");
        registry.add("security.jwt.issuer", () -> "https://api.noba.ma");
        registry.add("security.jwt.key-rotation-hours", () -> "24");
    }

    @Autowired
    protected UserRepository userRepository;

    /**
     * Password login is gated on a verified email (see {@link
     * ma.noba.identity.domain.service.PasswordAuthenticator}). Email/password signup leaves the
     * account unverified, and the verification token is only delivered out-of-band, so integration
     * tests that need a logged-in session mark the account verified directly.
     */
    protected void verifyEmail(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            // Signup creates the account as PENDING; the login gate (PasswordAuthenticator)
            // requires ACTIVE, so mark both the same way the real verify-email flow does.
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        });
    }
}
