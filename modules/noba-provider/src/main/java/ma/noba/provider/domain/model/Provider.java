package ma.noba.provider.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "provider")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Provider {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    private String logoUrl;

    private String address;

    private Double latitude;

    private Double longitude;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderStatus status;

    @Column(nullable = false)
    private boolean verified;

    private Instant verificationDate;

    @Column(name = "priority_queue_enabled", nullable = false)
    private boolean priorityQueueEnabled;

    @Column(name = "wheelchair_accessible", nullable = false)
    private boolean wheelchairAccessible;

    @Column(name = "sign_language", nullable = false)
    private boolean signLanguage;

    @Column(name = "sensory_friendly", nullable = false)
    private boolean sensoryFriendly;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProviderService> services = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Provider(UUID userId, String name, String category) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.status = ProviderStatus.PENDING;
        this.verified = false;
        this.priorityQueueEnabled = false;
        this.wheelchairAccessible = false;
        this.signLanguage = false;
        this.sensoryFriendly = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Provider create(UUID userId, String name, String category) {
        return new Provider(userId, name, category);
    }

    public void addService(String serviceName, Duration duration, BigDecimal price, String category) {
        ProviderService service = new ProviderService(this, serviceName, duration, price);
        service.setCategory(category);
        this.services.add(service);
        this.updatedAt = Instant.now();
    }

    public void removeService(UUID serviceId) {
        this.services.removeIf(s -> s.getId().equals(serviceId));
        this.updatedAt = Instant.now();
    }

    public void updateInfo(String name, String description, String logoUrl, String address, String category) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (logoUrl != null) this.logoUrl = logoUrl;
        if (address != null) this.address = address;
        if (category != null) this.category = category;
        this.updatedAt = Instant.now();
    }

    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = ProviderStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = ProviderStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == ProviderStatus.ACTIVE;
    }

    public Optional<ProviderService> findService(UUID serviceId) {
        return this.services.stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst();
    }

    public List<ProviderService> getActiveServices() {
        return this.services.stream()
                .filter(ProviderService::isActive)
                .toList();
    }
}
