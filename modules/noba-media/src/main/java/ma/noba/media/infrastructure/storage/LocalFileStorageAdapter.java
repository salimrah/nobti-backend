package ma.noba.media.infrastructure.storage;

import ma.noba.media.application.storage.StorageProperties;

import ma.noba.media.domain.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Writes uploads under the configured directory; public files are served by /media/**. */
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private final StorageProperties props;
    private final Path root;

    public LocalFileStorageAdapter(StorageProperties props) {
        this.props = props;
        this.root = Path.of(props.getLocal().getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve("public"));
            Files.createDirectories(root.resolve("private"));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create media directory " + root, e);
        }
        log.info("Local media storage at {} (public base {})", root, props.getPublicBaseUrl());
    }

    @Override
    public StoredObject store(String objectKey, byte[] content, String contentType, Visibility visibility) {
        Path target = pathFor(objectKey, visibility);
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write upload " + objectKey, e);
        }
        String url = visibility == Visibility.PUBLIC
                ? props.getPublicBaseUrl() + "/" + objectKey
                : "/api/v1/storage/objects/" + objectKey;
        return new StoredObject(objectKey, url);
    }

    @Override
    public Optional<StoredContent> load(String objectKey) {
        for (Visibility visibility : Visibility.values()) {
            Path file = pathFor(objectKey, visibility);
            if (Files.exists(file)) {
                try {
                    return Optional.of(new StoredContent(contentType(objectKey), Files.readAllBytes(file)));
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to read upload " + objectKey, e);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(pathFor(objectKey, Visibility.PUBLIC));
            Files.deleteIfExists(pathFor(objectKey, Visibility.PRIVATE));
        } catch (IOException e) {
            log.warn("Unable to delete upload {}: {}", objectKey, e.getMessage());
        }
    }

    private Path pathFor(String objectKey, Visibility visibility) {
        Path resolved = root.resolve(visibility == Visibility.PUBLIC ? "public" : "private")
                .resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return resolved;
    }

    static String contentType(String key) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".heic")) return "image/heic";
        return "image/jpeg";
    }
}
