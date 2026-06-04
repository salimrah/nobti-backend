package ma.noba.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("domainEvents")
    public Executor domainEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("domain-event-");
        executor.setRejectedExecutionHandler((r, e) -> 
            log.error("Domain event task rejected: task={}, poolSize={}, queueSize={}", 
                    r, e.getPoolSize(), e.getQueue().size()));
        executor.initialize();
        return executor;
    }

    @Bean("notifications")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler((r, e) ->
            log.error("Notification task rejected: task={}, poolSize={}, queueSize={}",
                    r, e.getPoolSize(), e.getQueue().size()));
        executor.initialize();
        return executor;
    }
}
