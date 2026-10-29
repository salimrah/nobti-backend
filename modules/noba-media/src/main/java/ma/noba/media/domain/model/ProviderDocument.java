package ma.noba.media.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A verification document uploaded by a provider. */
@Entity
@Table(name = "provider_document")
@Getter
@Setter
@NoArgsConstructor
public class ProviderDocument {

    @Id
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "reviewer_note", length = 500)
    private String reviewerNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
    }

    public void review(String newStatus, String note, UUID reviewer) {
        this.status = newStatus;
        this.reviewerNote = note == null ? null : note.substring(0, Math.min(500, note.length()));
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
    }
}
