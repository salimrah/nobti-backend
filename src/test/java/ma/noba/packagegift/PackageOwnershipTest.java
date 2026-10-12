package ma.noba.packagegift;

import ma.noba.packagegift.application.service.PackageApplicationService;
import ma.noba.packagegift.domain.model.ServicePackage;
import ma.noba.packagegift.domain.port.ServicePackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageOwnershipTest {

    @Mock ServicePackageRepository packageRepository;

    private PackageApplicationService service;

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PackageApplicationService(packageRepository);
    }

    private ServicePackage pkg() {
        return new ServicePackage(providerId, "Bundle", new BigDecimal("100.00"), new BigDecimal("150.00"), 30);
    }

    @Test
    @DisplayName("A foreign provider cannot deactivate a package")
    void foreignProviderCannotDeactivatePackage() {
        ServicePackage pkg = pkg();
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> service.deactivatePackage(id, otherProviderId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(pkg.isActive()).isTrue();
        verify(packageRepository, never()).save(any());
    }

    @Test
    @DisplayName("A foreign provider cannot update a package")
    void foreignProviderCannotUpdatePackage() {
        ServicePackage pkg = pkg();
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> service.updatePackage(id, otherProviderId,
                new ma.noba.packagegift.application.dto.UpdatePackageRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(packageRepository, never()).save(any());
    }
}
