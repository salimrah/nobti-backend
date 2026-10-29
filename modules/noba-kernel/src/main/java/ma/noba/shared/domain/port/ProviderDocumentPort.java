package ma.noba.shared.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Provider verification documents (KYC), reviewed from the admin console. */
public interface ProviderDocumentPort {

    List<DocumentRow> list(UUID providerId);

    List<DocumentRow> pending();

    Optional<DocumentRow> find(UUID documentId);

    DocumentRow submit(UUID providerId, String type, String objectKey, String fileName,
                       String contentType, long sizeBytes);

    DocumentRow review(UUID documentId, String status, String note, UUID reviewerId);

    void delete(UUID documentId);

    Optional<DocumentFile> file(UUID documentId);

    record DocumentRow(
            UUID id,
            UUID providerId,
            String type,
            String fileName,
            String contentType,
            long sizeBytes,
            String status,
            String reviewerNote,
            Instant createdAt,
            Instant reviewedAt) {
    }

    record DocumentFile(String objectKey, String fileName, String contentType) {
    }
}
