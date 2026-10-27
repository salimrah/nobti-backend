package ma.noba.app.events;

import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.model.OutboxEvent;
import ma.noba.shared.domain.port.OutboxEventRepository;
import ma.noba.shared.infra.event.RabbitMqEventBus;
import ma.noba.shared.infra.lock.RedisDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Component
@Conditional(OutboxPoller.OutboxAndRabbitMqEnabled.class)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int BATCH_SIZE = 50;
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final OutboxEventRepository outboxRepository;
    private final RabbitMqEventBus rabbitMqEventBus;
    private final EventDeserializer deserializer;
    private final RedisDistributedLock distributedLock;

    public OutboxPoller(OutboxEventRepository outboxRepository, RabbitMqEventBus rabbitMqEventBus, RedisDistributedLock distributedLock) {
        this.outboxRepository = outboxRepository;
        this.rabbitMqEventBus = rabbitMqEventBus;
        this.deserializer = new EventDeserializer();
        this.distributedLock = distributedLock;
    }

    static class OutboxAndRabbitMqEnabled extends AllNestedConditions {
        OutboxAndRabbitMqEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = "event.outbox.enabled", havingValue = "true")
        static class OutboxEnabled {}

        @ConditionalOnProperty(name = "event.bus.rabbitmq.enabled", havingValue = "true")
        static class RabbitMqEnabled {}
    }

    @Scheduled(fixedDelayString = "${event.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        if (!distributedLock.tryLock("outbox-poller", LOCK_TTL)) {
            return;
        }

        try {
            List<OutboxEvent> pending = outboxRepository.findPending(BATCH_SIZE);
            if (pending.isEmpty()) {
                return;
            }

            log.info("Processing {} outbox events", pending.size());

            for (OutboxEvent outboxEvent : pending) {
                try {
                    DomainEvent event = deserializer.deserialize(outboxEvent);
                    rabbitMqEventBus.publish(event);
                    outboxEvent.markSent();
                    outboxRepository.save(outboxEvent);
                    log.debug("Published outbox event: {} [{}]", outboxEvent.getEventType(), outboxEvent.getEventId());
                } catch (Exception e) {
                    outboxEvent.markFailed(e.getMessage());
                    outboxRepository.save(outboxEvent);
                    log.error("Failed to publish outbox event: {} [{}]",
                            outboxEvent.getEventType(), outboxEvent.getEventId(), e);
                }
            }
        } finally {
            distributedLock.release("outbox-poller");
        }
    }
}
