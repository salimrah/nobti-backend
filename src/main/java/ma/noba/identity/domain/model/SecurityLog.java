package ma.noba.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_logs", indexes = {
        @Index(name = "idx_security_logs_user_time", columnList = "userId, createdAt DESC"),
        @Index(name = "idx_security_logs_type_time", columnList = "eventType, createdAt DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    private String eventType;
    private String ipAddress;
    private String userAgent;

    private String payload;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
