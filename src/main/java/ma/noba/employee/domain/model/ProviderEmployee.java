package ma.noba.employee.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_employee")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderEmployee {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    private UUID userId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String role;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderEmployee(UUID providerId, String name, String role) {
        this.providerId = providerId;
        this.name = name;
        this.role = role;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void assignLocation(UUID locationId) {
        this.locationId = locationId;
        this.updatedAt = Instant.now();
    }

    public void removeLocation() {
        this.locationId = null;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public void update(String name, String role) {
        this.name = name;
        this.role = role;
        this.updatedAt = Instant.now();
    }
}
