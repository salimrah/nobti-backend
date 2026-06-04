package ma.noba.shared;

import java.util.function.Supplier;

public interface IdempotencyCache {

    /**
     * Put a value if absent, with TTL.
     * @return existing or newly set value
     */
    String putIfAbsent(String key, String value, int ttlSeconds);

    /**
     * Set or overwrite a value with TTL.
     */
    void set(String key, String value, int ttlSeconds);

    /**
     * Get a value by key.
     */
    String get(String key);

    /**
     * Utility: Execute supplier if key not cached.
     */
    <T> T getOrExecute(String key, int ttlSeconds, Supplier<T> supplier);
}

