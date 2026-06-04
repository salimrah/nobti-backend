package ma.noba.queue.infra.listener;

import ma.noba.provider.domain.model.ProviderService;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.queue.domain.event.ClientServiceCompletedEvent;
import ma.noba.user.domain.model.UserServiceHistory;
import ma.noba.user.domain.port.UserServiceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class ServiceHistoryListener {

    private static final Logger log = LoggerFactory.getLogger(ServiceHistoryListener.class);

    private final UserServiceHistoryRepository historyRepository;
    private final ProviderRepository providerRepository;

    public ServiceHistoryListener(UserServiceHistoryRepository historyRepository, ProviderRepository providerRepository) {
        this.historyRepository = historyRepository;
        this.providerRepository = providerRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServiceCompleted(ClientServiceCompletedEvent event) {
        log.info("Service completed for client {} at provider {} - saving to history",
                event.clientId(), event.providerId());

        var provider = providerRepository.findById(event.providerId());
        String providerName = provider.map(p -> p.getName()).orElse("Unknown Provider");

        String serviceName = null;
        if (event.serviceId() != null && provider.isPresent()) {
            serviceName = provider.get().getServices().stream()
                    .filter(s -> s.getId().equals(UUID.fromString(event.serviceId())))
                    .findFirst()
                    .map(ProviderService::getName)
                    .orElse(null);
        }

        UserServiceHistory history = new UserServiceHistory(
                event.clientId(),
                event.providerId(),
                providerName,
                event.serviceId(),
                serviceName,
                java.time.Instant.now()
        );

        historyRepository.save(history);
        log.debug("Saved service history entry for user {}", event.clientId());
    }
}
