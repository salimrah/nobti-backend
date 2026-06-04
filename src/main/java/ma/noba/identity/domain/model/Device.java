package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_user", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Device {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String deviceName;
    private String platform;
    private String appVersion;
    private String fcmToken;
    private String lastIpAddress;
    private String lastUserAgent;
    private boolean trusted;
    @Builder.Default private int trustScore = 0;
    private Instant lastSeenAt;
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
