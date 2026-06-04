package ma.noba.packagegift.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_package")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ServicePackage {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "valid_days", nullable = false)
    private int validDays = 30;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "servicePackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PackageServiceItem> serviceItems = new ArrayList<>();

    public ServicePackage(UUID providerId, String name, BigDecimal price, BigDecimal originalPrice, int validDays) {
        this.providerId = providerId;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.validDays = validDays;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addServiceItem(UUID serviceId, int quantity) {
        this.serviceItems.add(new PackageServiceItem(this, serviceId, quantity));
        this.updatedAt = Instant.now();
    }

    public void update(String name, String description, BigDecimal price, BigDecimal originalPrice, int validDays) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (originalPrice != null) this.originalPrice = originalPrice;
        if (validDays > 0) this.validDays = validDays;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }
}
