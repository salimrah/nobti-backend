package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.port.ProviderServicePricePort;
import ma.noba.provider.domain.model.ProviderService;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderServicePriceAdapter implements ProviderServicePricePort {

    private final ProviderRepository providerRepository;

    @Override
    public Optional<BigDecimal> findActiveServicePrice(UUID providerId, UUID serviceId) {
        return providerRepository.findById(providerId)
                .flatMap(provider -> provider.findService(serviceId))
                .filter(ProviderService::isActive)
                .map(ProviderService::getPrice)
                .filter(Objects::nonNull);
    }
}
