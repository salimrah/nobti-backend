package ma.noba.payment.infra;

import ma.noba.payment.domain.model.ProviderPayoutSettings;
import ma.noba.payment.domain.port.ProviderPayoutSettingsRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaProviderPayoutSettingsRepository extends JpaRepository<ProviderPayoutSettings, UUID>, ProviderPayoutSettingsRepository {

    Optional<ProviderPayoutSettings> findByProviderId(UUID providerId);

    Optional<ProviderPayoutSettings> findByStripeAccountId(String stripeAccountId);
}
