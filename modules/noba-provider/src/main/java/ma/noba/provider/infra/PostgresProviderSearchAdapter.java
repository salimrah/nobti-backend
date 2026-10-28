package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.port.ProviderSearchPort;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Default search: Postgres full-text (name weighted over description). */
@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "postgres", matchIfMissing = true)
@RequiredArgsConstructor
public class PostgresProviderSearchAdapter implements ProviderSearchPort {

    private final ProviderRepository providerRepository;

    @Override
    public Page<Provider> search(String query, Pageable pageable) {
        return providerRepository.searchActive(query, pageable);
    }
}
