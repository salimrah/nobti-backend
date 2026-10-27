package ma.noba.promotion.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Promotion {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validTo;

    private Integer maxRedemptions;

    @Column(nullable = false)
    private int currentRedemptions;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "promotion_service", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "service_id")
    private List<UUID> serviceIds;

    public Promotion(UUID providerId, String title, String description, BigDecimal discountPercent, Instant validFrom, Instant validTo, Integer maxRedemptions, List<UUID> serviceIds) {
        this.providerId = providerId;
        this.title = title;
        this.description = description;
        this.discountPercent = discountPercent;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.maxRedemptions = maxRedemptions;
        this.serviceIds = serviceIds;
        this.currentRedemptions = 0;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public boolean isValid() {
        Instant now = Instant.now();
        return isActive && now.isAfter(validFrom) && now.isBefore(validTo)
            && (maxRedemptions == null || currentRedemptions < maxRedemptions);
    }

    public void redeem() {
        if (!isValid()) {
            throw new IllegalStateException("Promotion is no longer valid");
        }
        this.currentRedemptions++;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
