package ma.noba.provider.infra.listener;

import ma.noba.provider.domain.event.ProviderCreatedEvent;
import ma.noba.provider.domain.event.ProviderRatedEvent;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.event.ProviderUpdatedEvent;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderSearchView;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.provider.infra.JpaProviderRatingRepository;
import ma.noba.provider.infra.ProviderSearchViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class ProviderSearchViewSyncListener {

    private static final Logger log = LoggerFactory.getLogger(ProviderSearchViewSyncListener.class);

    private final ProviderSearchViewRepository searchViewRepository;
    private final JpaProviderRatingRepository ratingRepository;
    private final ProviderRepository providerRepository;

    public ProviderSearchViewSyncListener(
            ProviderSearchViewRepository searchViewRepository,
            JpaProviderRatingRepository ratingRepository,
            ProviderRepository providerRepository) {
        this.searchViewRepository = searchViewRepository;
        this.ratingRepository = ratingRepository;
        this.providerRepository = providerRepository;
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderCreated(ProviderCreatedEvent event) {
        providerRepository.findById(event.providerId()).ifPresent(this::syncProvider);
        log.info("ProviderSearchView synced for created provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderUpdated(ProviderUpdatedEvent event) {
        providerRepository.findById(event.providerId()).ifPresent(this::syncProvider);
        log.info("ProviderSearchView synced for updated provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderDeleted(ProviderUpdatedEvent event) {
        searchViewRepository.deleteById(event.providerId());
        log.info("ProviderSearchView removed for deleted provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderStatusChanged(ProviderStatusChangedEvent event) {
        providerRepository.findById(event.providerId()).ifPresent(this::syncProvider);
        log.info("ProviderSearchView synced for status change of provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRatingChanged(ProviderRatedEvent event) {
        searchViewRepository.findById(event.providerId()).ifPresent(view -> {
            double avg = ratingRepository.getAverageRating(event.providerId());
            long count = ratingRepository.findByProviderId(event.providerId()).size();
            view.setRatingAverage(avg);
            view.setRatingCount((int) count);
            view.setUpdatedAt(Instant.now());
            searchViewRepository.save(view);
            log.info("ProviderSearchView rating synced for provider: {}", event.providerId());
        });
    }

    private void syncProvider(Provider provider) {
        ProviderSearchView view = searchViewRepository.findById(provider.getId())
                .orElse(new ProviderSearchView());
        view.setId(provider.getId());
        view.setName(provider.getName());
        view.setCategory(provider.getCategory());
        view.setActive(provider.getStatus() == ma.noba.provider.domain.model.ProviderStatus.ACTIVE);
        view.setUpdatedAt(Instant.now());

        double avg = ratingRepository.getAverageRating(provider.getId());
        long count = ratingRepository.findByProviderId(provider.getId()).size();
        view.setRatingAverage(avg);
        view.setRatingCount((int) count);

        searchViewRepository.save(view);
    }
}
