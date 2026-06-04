package ma.noba.user.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_service_history")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserServiceHistory {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    @Column(length = 200)
    private String providerName;

    @Column(name = "service_id")
    private String serviceId;

    @Column(length = 200)
    private String serviceName;

    @Column(nullable = false)
    private Instant completedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer rating;

    @Column(nullable = false)
    private Instant createdAt;

    public UserServiceHistory(UUID userId, UUID providerId, String providerName, String serviceId, String serviceName, Instant completedAt) {
        this.userId = userId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.completedAt = completedAt;
        this.createdAt = Instant.now();
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
