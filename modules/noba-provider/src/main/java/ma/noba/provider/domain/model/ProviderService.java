package ma.noba.provider.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_service")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderService {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore
    private Provider provider;

    @Column(nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(nullable = false, columnDefinition = "INTERVAL")
    private Duration duration;

    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Instant createdAt;

    public ProviderService(Provider provider, String name, Duration duration, BigDecimal price) {
        this.provider = provider;
        this.name = name;
        this.duration = duration;
        this.price = price;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }

    public void update(String name, Duration duration, BigDecimal price) {
        if (name != null) this.name = name;
        if (duration != null) this.duration = duration;
        if (price != null) this.price = price;
    }
}
