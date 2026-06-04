package ma.noba.provider.infra.adapter;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.provider.infra.JpaProviderRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderLookupAdapter implements ProviderLookupPort {

    private final JpaProviderRepository jpaProviderRepository;

    @Override
    public Optional<UUID> findProviderIdByUserId(UUID userId) {
        return jpaProviderRepository.findByUserId(userId)
            .map(provider -> provider.getId());
    }
}
