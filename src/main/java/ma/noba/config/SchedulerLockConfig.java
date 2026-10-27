package ma.noba.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Cross-instance locking for {@code @Scheduled} jobs.
 *
 * <p>Spring's scheduler runs on every application instance, which would duplicate reminders,
 * cleanups and outbox polling as soon as the backend is scaled beyond one replica. ShedLock
 * stores a row per job in Postgres so only the instance that acquires the lock executes it.
 *
 * <p>Disabled in the integration-test profile, where the schema is created by Hibernate and the
 * {@code shedlock} table does not exist.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
@ConditionalOnProperty(name = "shedlock.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
