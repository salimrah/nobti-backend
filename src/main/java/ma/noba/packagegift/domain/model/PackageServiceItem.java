package ma.noba.packagegift.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "package_service_item")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PackageServiceItem {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private int quantity = 1;

    public PackageServiceItem(ServicePackage servicePackage, UUID serviceId, int quantity) {
        this.servicePackage = servicePackage;
        this.serviceId = serviceId;
        this.quantity = quantity;
    }
}
