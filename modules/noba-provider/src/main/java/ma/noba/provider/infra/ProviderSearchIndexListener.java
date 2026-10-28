package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.provider.domain.event.ProviderCreatedEvent;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.event.ProviderUpdatedEvent;
import ma.noba.provider.domain.port.ProviderSearchIndexPort;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Keeps the external search index in sync with provider changes. Only active with an external
 * search engine; Postgres queries live data.
 */
@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "meilisearch")
@RequiredArgsConstructor
public class ProviderSearchIndexListener {

    private final ProviderSearchIndexPort searchIndex;
    private final ProviderRepository providerRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderCreated(ProviderCreatedEvent event) {
        reindex(event.providerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderUpdated(ProviderUpdatedEvent event) {
        reindex(event.providerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderStatusChanged(ProviderStatusChangedEvent event) {
        reindex(event.providerId());
    }

    private void reindex(UUID providerId) {
        providerRepository.findById(providerId).ifPresent(searchIndex::index);
    }
}
