package ma.noba.shared.infra.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.model.OutboxEvent;
import ma.noba.shared.domain.port.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Outbox logic is now handled by {@link CompositeEventBus}.
 *             This class is kept for reference and will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class OutboxEventPublisher implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final EventBus delegate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxRepository, EventBus delegate) {
        this.outboxRepository = outboxRepository;
        this.delegate = delegate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.pending(
                    event.eventId(),
                    event.eventType(),
                    extractAggregateId(event),
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.debug("Event saved to outbox: {} [{}]", event.eventType(), event.eventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to outbox: {}", event.eventType(), e);
            delegate.publish(event);
        }
    }

    private String extractAggregateId(DomainEvent event) {
        try {
            Object aggregateId = event.getClass().getMethod("aggregateId").invoke(event);
            return aggregateId != null ? aggregateId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
