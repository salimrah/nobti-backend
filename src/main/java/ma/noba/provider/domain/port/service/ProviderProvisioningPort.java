package ma.noba.provider.domain.port.service;

import java.util.UUID;

public interface ProviderProvisioningPort {
    void onProviderCreated(UUID userId, String firstName, String lastName);
}
