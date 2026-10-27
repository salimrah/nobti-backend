package ma.noba.provider.infra.listener;

import ma.noba.config.CacheInvalidationPublisher;
import ma.noba.provider.domain.event.ProviderCreatedEvent;
import ma.noba.provider.domain.event.ProviderRatedEvent;
import ma.noba.provider.domain.event.ProviderUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class CacheInvalidationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationEventListener.class);

    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public CacheInvalidationEventListener(CacheInvalidationPublisher cacheInvalidationPublisher) {
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderCreated(ProviderCreatedEvent event) {
        cacheInvalidationPublisher.evictAll("providers");
        cacheInvalidationPublisher.evict("providerDetails", event.providerId().toString());
        log.info("Cache invalidated for created provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderUpdated(ProviderUpdatedEvent event) {
        cacheInvalidationPublisher.evictAll("providers");
        cacheInvalidationPublisher.evict("providerDetails", event.providerId().toString());
        log.info("Cache invalidated for updated provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderDeleted(ProviderUpdatedEvent event) {
        cacheInvalidationPublisher.evictAll("providers");
        cacheInvalidationPublisher.evict("providerDetails", event.providerId().toString());
        log.info("Cache invalidated for deleted provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRatingChanged(ProviderRatedEvent event) {
        cacheInvalidationPublisher.evict("ratings", event.providerId().toString());
        cacheInvalidationPublisher.evict("providerDetails", event.providerId().toString());
        log.info("Cache invalidated for rating change on provider: {}", event.providerId());
    }

    @Async("domainEvents")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServicesChanged(ProviderUpdatedEvent event) {
        cacheInvalidationPublisher.evictAll("services");
        cacheInvalidationPublisher.evict("providerDetails", event.providerId().toString());
        log.info("Cache invalidated for services change on provider: {}", event.providerId());
    }
}
