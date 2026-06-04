package ma.noba.provider.infra;

import ma.noba.provider.application.service.ProviderDiscoveryService;
import ma.noba.queue.domain.port.service.BusinessHoursPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProviderBusinessHoursAdapter implements BusinessHoursPort {

    private final ProviderDiscoveryService discoveryService;

    public ProviderBusinessHoursAdapter(ProviderDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public boolean isOpenNow(UUID providerId) {
        return discoveryService.isOpenNow(providerId);
    }
}
