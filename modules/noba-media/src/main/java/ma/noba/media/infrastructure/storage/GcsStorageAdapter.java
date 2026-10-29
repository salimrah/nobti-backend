package ma.noba.media.infrastructure.storage;

import ma.noba.media.application.storage.StorageProperties;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import ma.noba.media.domain.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Google Cloud Storage adapter. Uses the VM's service account (ADC) by default. */
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "gcs")
public class GcsStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageAdapter.class);

    private final StorageProperties props;
    private final Storage storage;
    private final String bucket;

    public GcsStorageAdapter(StorageProperties props) {
        this.props = props;
        this.bucket = props.getGcs().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("storage.gcs.bucket must be set when storage.provider=gcs");
        }
        this.storage = StorageOptions.getDefaultInstance().getService();
        log.info("GCS media storage bucket {}", bucket);
    }

    @Override
    public StoredObject store(String objectKey, byte[] content, String contentType, Visibility visibility) {
        BlobInfo.Builder builder = BlobInfo.newBuilder(BlobId.of(bucket, objectKey))
                .setContentType(contentType)
                .setCacheControl("public, max-age=31536000");
        if (visibility == Visibility.PUBLIC) {
            builder.setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        storage.create(builder.build(), content);
        String base = props.getGcs().getPublicBaseUrl() != null && !props.getGcs().getPublicBaseUrl().isBlank()
                ? props.getGcs().getPublicBaseUrl()
                : "https://storage.googleapis.com/" + bucket;
        String url = visibility == Visibility.PUBLIC
                ? base + "/" + objectKey
                : "/api/v1/storage/objects/" + objectKey;
        return new StoredObject(objectKey, url);
    }

    @Override
    public Optional<StoredContent> load(String objectKey) {
        var blob = storage.get(BlobId.of(bucket, objectKey));
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        return Optional.of(new StoredContent(blob.getContentType(), blob.getContent()));
    }

    @Override
    public void delete(String objectKey) {
        storage.delete(BlobId.of(bucket, objectKey));
    }
}
