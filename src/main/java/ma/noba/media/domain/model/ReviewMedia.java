package ma.noba.media.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_media")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReviewMedia {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID reviewId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 20)
    private String mediaType;

    @Column(nullable = false)
    private boolean isModerated;

    @Column(nullable = false)
    private Instant uploadedAt;

    public ReviewMedia(UUID reviewId, String url, String mediaType) {
        this.reviewId = reviewId;
        this.url = url;
        this.mediaType = mediaType != null ? mediaType : "IMAGE";
        this.isModerated = false;
        this.uploadedAt = Instant.now();
    }

    public void markModerated() {
        this.isModerated = true;
    }
}
