package ma.noba.shared.infra.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.model.OutboxEvent;
import ma.noba.shared.domain.port.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(CompositeEventBus.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RabbitMqEventBus rabbitMqEventBus;
    private final OutboxEventRepository outboxRepository;
    private final EventIdempotencyStore idempotencyStore;
    private final boolean rabbitMqEnabled;
    private final boolean outboxEnabled;
    private final ObjectMapper objectMapper;

    public CompositeEventBus(
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) RabbitMqEventBus rabbitMqEventBus,
            @Autowired(required = false) OutboxEventRepository outboxRepository,
            @Autowired(required = false) EventIdempotencyStore idempotencyStore,
            @Value("${event.bus.rabbitmq.enabled:false}") boolean rabbitMqEnabled,
            @Value("${event.outbox.enabled:false}") boolean outboxEnabled) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.rabbitMqEventBus = rabbitMqEventBus;
        this.outboxRepository = outboxRepository;
        this.idempotencyStore = idempotencyStore;
        this.rabbitMqEnabled = rabbitMqEnabled;
        this.outboxEnabled = outboxEnabled;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);

        if (rabbitMqEnabled && idempotencyStore != null) {
            idempotencyStore.markInstanceLocal(event.eventId().toString());
        }

        if (!rabbitMqEnabled || rabbitMqEventBus == null) {
            return;
        }

        if (outboxEnabled && outboxRepository != null) {
            saveToOutbox(event);
        } else {
            rabbitMqEventBus.publish(event);
        }
    }

    private void saveToOutbox(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.pending(
                    event.eventId(),
                    event.eventType(),
                    extractAggregateId(event),
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.debug("Saved event to outbox: {} [{}]", event.eventType(), event.eventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to outbox: {}", event.eventType(), e);
            rabbitMqEventBus.publish(event);
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
