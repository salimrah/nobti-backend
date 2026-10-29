package ma.noba.media.domain.port;

import java.util.Optional;

/** Object storage for user uploads (local disk in development, GCS in production). */
public interface StoragePort {

    enum Visibility { PUBLIC, PRIVATE }

    StoredObject store(String objectKey, byte[] content, String contentType, Visibility visibility);

    Optional<StoredContent> load(String objectKey);

    void delete(String objectKey);

    record StoredObject(String key, String url) {
    }

    record StoredContent(String contentType, byte[] content) {
    }
}
