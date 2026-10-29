package ma.noba.media.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.media.domain.port.StoragePort;
import ma.noba.media.application.storage.StorageProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Upload endpoint shared by the mobile app (avatars, portfolio, reviews, documents). */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "application/pdf");
    private static final Set<String> KINDS = Set.of(
            "avatars", "portfolio", "reviews", "documents", "general");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "image/gif", "gif", "image/heic", "heic", "application/pdf", "pdf");

    private final StoragePort storage;
    private final StorageProperties props;

    public record UploadResponse(String key, String url, String contentType, long size) {
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(defaultValue = "general") String kind) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        if (file.getSize() > props.getMaxUploadBytes()) {
            throw new IllegalArgumentException("File too large (max " + props.getMaxUploadBytes() + " bytes)");
        }
        String rawType = file.getContentType();
        String contentType = rawType == null
                ? "application/octet-stream" : rawType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
        String normalizedKind = kind.trim().toLowerCase(Locale.ROOT);
        if (!KINDS.contains(normalizedKind)) {
            throw new IllegalArgumentException("Unsupported upload kind: " + kind);
        }
        String extension = EXTENSIONS.getOrDefault(contentType, "bin");
        String month = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC).format(Instant.now());
        String objectKey = normalizedKind + "/" + month + "/" + UUID.randomUUID() + "." + extension;
        StoragePort.Visibility visibility = "documents".equals(normalizedKind)
                ? StoragePort.Visibility.PRIVATE
                : StoragePort.Visibility.PUBLIC;

        StoragePort.StoredObject stored = storage.store(objectKey, file.getBytes(), contentType, visibility);
        return ResponseEntity.ok(new UploadResponse(stored.key(), stored.url(), contentType, file.getSize()));
    }

    /** Streams a private object (documents) to an authenticated caller. */
    @GetMapping("/objects/{*key}")
    public ResponseEntity<byte[]> download(@PathVariable String key) {
        String objectKey = key.startsWith("/") ? key.substring(1) : key;
        return storage.load(objectKey)
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(content.contentType()))
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                        .body(content.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
