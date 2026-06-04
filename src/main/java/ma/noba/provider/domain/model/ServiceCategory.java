package ma.noba.provider.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_category")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ServiceCategory {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(nullable = false, columnDefinition = "INTERVAL")
    private Duration defaultDuration;

    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    public ServiceCategory(String name, Duration defaultDuration, String description) {
        this.name = name;
        this.defaultDuration = defaultDuration;
        this.description = description;
        this.createdAt = Instant.now();
    }
}
