package ma.noba.identity.infrastructure;

import ma.noba.shared.IdempotencyCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryIdempotencyCache implements IdempotencyCache {

    private final Map<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public String putIfAbsent(String key, String value, int ttlSeconds) {
        return (String) store.computeIfAbsent(key, k -> value);
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        store.put(key, value);
    }

    @Override
    public String get(String key) {
        Object value = store.get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrExecute(String key, int ttlSeconds, Supplier<T> supplier) {
        return (T) store.computeIfAbsent(key, k -> supplier.get());
    }
}

