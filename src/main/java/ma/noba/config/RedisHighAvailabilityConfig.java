package ma.noba.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.sentinel.master")
public class RedisHighAvailabilityConfig {

    @Value("${spring.data.redis.sentinel.master}")
    private String master;

    @Value("${spring.data.redis.sentinel.nodes}")
    private String nodes;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(master);
        for (String node : nodes.split(",")) {
            String[] parts = node.trim().split(":");
            sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
        }
        return new LettuceConnectionFactory(sentinelConfig);
    }
}
