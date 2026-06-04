package ma.noba.media.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_media")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderMedia {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 500)
    private String caption;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private int mediaOrder;

    @Column(nullable = false)
    private Instant createdAt;

    public ProviderMedia(UUID providerId, String url, String caption, String category, int mediaOrder) {
        this.providerId = providerId;
        this.url = url;
        this.caption = caption;
        this.category = category;
        this.mediaOrder = mediaOrder;
        this.createdAt = Instant.now();
    }

    public void updateOrder(int mediaOrder) {
        this.mediaOrder = mediaOrder;
    }

    public void updateCaption(String caption) {
        this.caption = caption;
    }
}
