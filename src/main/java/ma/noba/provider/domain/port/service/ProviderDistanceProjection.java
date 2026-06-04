package ma.noba.provider.domain.port.service;

import java.util.UUID;

public interface ProviderDistanceProjection {
    UUID getId();
    Double getDistanceKm();
}
