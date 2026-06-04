package ma.noba.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        return template;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("providers", defaultCacheConfig(serializer, Duration.ofMinutes(10)));
        cacheConfigurations.put("providerDetails", defaultCacheConfig(serializer, Duration.ofMinutes(10)));
        cacheConfigurations.put("queueStatus", defaultCacheConfig(serializer, Duration.ofSeconds(30)));
        cacheConfigurations.put("services", defaultCacheConfig(serializer, Duration.ofMinutes(5)));
        cacheConfigurations.put("ratings", defaultCacheConfig(serializer, Duration.ofMinutes(5)));
        cacheConfigurations.put("otpTemplates", defaultCacheConfig(serializer, Duration.ofHours(1)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig(serializer, Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(2)));
        caffeineCacheManager.setCacheNames(Set.of(
                "providers", "providerDetails", "queueStatus", "services", "ratings", "otpTemplates"
        ));

        org.springframework.cache.support.CompositeCacheManager composite =
                new org.springframework.cache.support.CompositeCacheManager(redisCacheManager, caffeineCacheManager);
        composite.setFallbackToNoOpCache(true);
        return composite;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(2)));
        cacheManager.setCacheNames(Set.of(
                "providers", "providerDetails", "queueStatus", "services", "ratings", "otpTemplates"
        ));
        return cacheManager;
    }

    private ObjectMapper redisObjectMapper() {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    private RedisCacheConfiguration defaultCacheConfig(GenericJackson2JsonRedisSerializer serializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }
}
