package ma.noba.announcement.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcement")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Announcement {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean important;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    public Announcement(UUID providerId, String title, String message, boolean important, Instant expiresAt) {
        this.providerId = providerId;
        this.title = title;
        this.message = message;
        this.important = important;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }
}
