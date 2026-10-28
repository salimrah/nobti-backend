package ma.noba.provider.infra.adapter;

import ma.noba.provider.application.service.ProviderApplicationService;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminProviderPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
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
        return providerRepository.countByStatus(ProviderStatus.PENDING);
    }

    @Override
    public List<PendingProvider> pendingProviders() {
        return providerRepository.findByStatus(ProviderStatus.PENDING).stream()
                .map(p -> new PendingProvider(
                        p.getId(), p.getUserId(), p.getName(), p.getCategory(),
                        p.getStatus() != null ? p.getStatus().name() : null, p.getCreatedAt()))
                .toList();
    }

    @Override
    public PagedResponse<ProviderRow> searchProviders(String query, String status, int page, int size) {
        ProviderStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = ProviderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Unknown status filter: treat as "all".
            }
        }
        Page<Provider> result = providerRepository.adminSearch(
                query, parsed, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PagedResponse.of(
                result.getContent().stream().map(AdminProviderPortAdapter::toRow).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    private static ProviderRow toRow(Provider provider) {
        return new ProviderRow(
                provider.getId(),
                provider.getUserId(),
                provider.getName(),
                provider.getCategory(),
                provider.getStatus() != null ? provider.getStatus().name() : null,
                provider.getAddress(),
                provider.isVerified(),
                provider.getCreatedAt());
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
