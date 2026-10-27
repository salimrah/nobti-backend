package ma.noba.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @ConditionalOnExpression("'${app.datasource.replica.url:}' != ''")
    public DataSource replicaDataSource(
            @Value("${app.datasource.replica.url}") String url,
            @Value("${app.datasource.replica.username:}") String username,
            @Value("${app.datasource.replica.password:}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setReadOnly(true);
        config.setAutoCommit(false);

        log.info("Configuring read-replica datasource");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    @ConditionalOnExpression("'${app.datasource.replica.url:}' != ''")
    public DataSource routingDataSource(DataSourceProperties primaryProps,
                                        DataSource replicaDataSource) {
        log.info("Enabling read-replica routing (replica datasource configured)");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primaryProps.determineUrl());
        config.setUsername(primaryProps.determineUsername());
        config.setPassword(primaryProps.determinePassword());
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setReadOnly(false);
        DataSource primary = new HikariDataSource(config);

        return new RoutingDataSource(primary, replicaDataSource);
    }
}
