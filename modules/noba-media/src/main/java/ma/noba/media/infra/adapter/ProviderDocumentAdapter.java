package ma.noba.media.infra.adapter;

import lombok.RequiredArgsConstructor;
import ma.noba.media.domain.model.ProviderDocument;
import ma.noba.media.domain.port.ProviderDocumentRepository;
import ma.noba.shared.domain.port.ProviderDocumentPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderDocumentAdapter implements ProviderDocumentPort {

    private static final java.util.Set<String> TYPES = java.util.Set.of(
            "ID_CARD", "PASSPORT", "BUSINESS_LICENSE", "TAX_CERTIFICATE", "INSURANCE", "OTHER");
    private static final java.util.Set<String> REVIEW_STATUSES = java.util.Set.of("APPROVED", "REJECTED");

    private final ProviderDocumentRepository documents;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRow> list(UUID providerId) {
        return documents.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(ProviderDocumentAdapter::toRow).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRow> pending() {
        return documents.findByStatusOrderByCreatedAtAsc("PENDING").stream()
                .map(ProviderDocumentAdapter::toRow).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentRow> find(UUID documentId) {
        return documents.findById(documentId).map(ProviderDocumentAdapter::toRow);
    }

    @Override
    @Transactional
    public DocumentRow submit(UUID providerId, String type, String objectKey, String fileName,
                              String contentType, long sizeBytes) {
        String normalizedType = type == null ? "OTHER" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported document type: " + type);
        }
        ProviderDocument document = new ProviderDocument();
        document.setProviderId(providerId);
        document.setType(normalizedType);
        document.setObjectKey(objectKey);
        document.setFileName(fileName == null || fileName.isBlank() ? "document" : fileName);
        document.setContentType(contentType == null ? "application/octet-stream" : contentType);
        document.setSizeBytes(sizeBytes);
        document.setStatus("PENDING");
        return toRow(documents.save(document));
    }

    @Override
    @Transactional
    public DocumentRow review(UUID documentId, String status, String note, UUID reviewerId) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!REVIEW_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }
        ProviderDocument document = documents.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));
        document.review(normalized, note, reviewerId);
        return toRow(documents.save(document));
    }

    @Override
    @Transactional
    public void delete(UUID documentId) {
        documents.findById(documentId).ifPresent(documents::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentFile> file(UUID documentId) {
        return documents.findById(documentId)
                .map(document -> new DocumentFile(
                        document.getObjectKey(), document.getFileName(), document.getContentType()));
    }

    private static DocumentRow toRow(ProviderDocument document) {
        return new DocumentRow(
                document.getId(),
                document.getProviderId(),
                document.getType(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getReviewerNote(),
                document.getCreatedAt(),
                document.getReviewedAt());
    }
}
