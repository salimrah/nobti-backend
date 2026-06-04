package ma.noba.provider.infra.adapter;

import ma.noba.provider.application.service.ProviderApplicationService;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.shared.domain.port.AdminProviderPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminProviderPortAdapter implements AdminProviderPort {

    private final ProviderRepository providerRepository;
    private final ProviderApplicationService providerApplicationService;

    public AdminProviderPortAdapter(ProviderRepository providerRepository,
                                    ProviderApplicationService providerApplicationService) {
        this.providerRepository = providerRepository;
        this.providerApplicationService = providerApplicationService;
    }

    @Override
    public long countPendingProviders() {
        return providerRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProviderStatus.PENDING)
                .count();
    }

    @Override
    public void approveProvider(UUID providerId) {
        providerApplicationService.approveProvider(providerId);
    }

    @Override
    public void rejectProvider(UUID providerId) {
        providerApplicationService.rejectProvider(providerId);
    }
}
