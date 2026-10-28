package ma.noba.admin.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only record of a mutating admin action (who, what, target, before/after). */
@Entity
@Table(name = "admin_audit_event")
@Getter
@NoArgsConstructor
public class AdminAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_label", length = 160)
    private String actorLabel;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static AdminAuditEvent of(UUID actorId, String actorLabel, String action,
                                     String targetType, UUID targetId, Map<String, Object> metadata) {
        AdminAuditEvent event = new AdminAuditEvent();
        event.actorId = actorId;
        event.actorLabel = actorLabel;
        event.action = action;
        event.targetType = targetType;
        event.targetId = targetId;
        event.metadata = metadata;
        return event;
    }
}
