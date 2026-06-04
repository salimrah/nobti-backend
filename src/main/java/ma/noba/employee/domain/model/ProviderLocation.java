package ma.noba.employee.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_location")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderLocation {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderLocation(UUID providerId, String name, String address) {
        this.providerId = providerId;
        this.name = name;
        this.address = address;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String name, String address, Double latitude, Double longitude, String phone) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = Instant.now();
    }
}
