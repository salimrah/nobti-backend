package ma.noba.user.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserProfile {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    private String profilePictureUrl;

    @Column(length = 10)
    private String preferredLanguage;

    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserProfile(UUID userId, String firstName, String lastName) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.preferredLanguage = "en";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String firstName, String lastName, String profilePictureUrl, String preferredLanguage) {
        if (firstName != null) this.firstName = firstName;
        if (lastName != null) this.lastName = lastName;
        if (profilePictureUrl != null) this.profilePictureUrl = profilePictureUrl;
        if (preferredLanguage != null) this.preferredLanguage = preferredLanguage;
        this.updatedAt = Instant.now();
    }
}
