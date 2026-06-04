package ma.noba.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;
import ma.noba.identity.infrastructure.crypto.AesGcmStringAttributeConverter;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users", indexes = {
        @Index(name = "uq_users_primary_email_hash", columnList = "primary_email_hash", unique = true),
        @Index(name = "uq_users_primary_phone_hash", columnList = "primary_phone_hash", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "primary_email")
    @Convert(converter = AesGcmStringAttributeConverter.class)
    private String email;

    @Column(name = "primary_email_hash")
    private String emailHash;

    @Column(name = "primary_phone")
    @Convert(converter = AesGcmStringAttributeConverter.class)
    private String phone;

    @Column(name = "primary_phone_hash")
    private String phoneHash;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "primary_email_verified")
    private boolean emailVerified;

    @Column(name = "primary_phone_verified")
    private boolean phoneVerified;

    private String timezone;
    private Instant lastLoginAt;
    @Builder.Default private int failedLoginAttempts = 0;
    private Instant lockedUntil;
    private Instant lastFailedLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status")
    private OnboardingStatus onboardingStatus;

    @PrePersist void prePersist(){
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = UserStatus.ACTIVE;
        if (timezone == null) timezone = "UTC";
        if (onboardingStatus == null) onboardingStatus = OnboardingStatus.NEW;
    }

    @PreUpdate void preUpdate(){ updatedAt = Instant.now(); }

    public boolean isActive(){ return status == UserStatus.ACTIVE; }
}
