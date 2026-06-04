package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_link_events", indexes = {
        @Index(name = "idx_account_link_events_user", columnList = "user_id"),
        @Index(name = "idx_account_link_events_actor", columnList = "actor_user_id"),
        @Index(name = "idx_account_link_events_time", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountLinkEvent {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    private String providerType;

    private String action;

    private String reason;

    private String payload;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
