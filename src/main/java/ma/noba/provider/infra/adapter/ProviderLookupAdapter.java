package ma.noba.provider.infra.adapter;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderLookupAdapter implements ProviderLookupPort {

    private final ProviderRepository providerRepository;

    @Override
    public Optional<UUID> findProviderIdByUserId(UUID userId) {
        return providerRepository.findByUserId(userId)
            .map(provider -> provider.getId());
    }

    @Override
    public Optional<String> findProviderName(UUID providerId) {
        return providerRepository.findById(providerId)
            .map(provider -> provider.getName());
    }
}
