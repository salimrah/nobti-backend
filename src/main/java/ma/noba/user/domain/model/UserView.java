package ma.noba.user.domain.model;

import jakarta.persistence.*;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_view")
public class UserView implements Persistable<UUID> {

    @Id
    private UUID id;

    @Version
    private Integer version;

    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status")
    private OnboardingStatus onboardingStatus;

    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected UserView() {}

    public UserView(UUID id, String email, String phone, UserStatus status, OnboardingStatus onboardingStatus, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.onboardingStatus = onboardingStatus;
        this.createdAt = createdAt;
        this.isNew = true;
    }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }

    @Override
    public boolean isNew() { return isNew; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserView that = (UserView) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public UUID getId() { return id; }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserStatus getStatus() { return status; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public void setOnboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(UserStatus status) { this.status = status; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
}
