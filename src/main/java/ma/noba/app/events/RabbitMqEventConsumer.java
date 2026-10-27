package ma.noba.app.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.identity.domain.event.OtpFailedEvent;
import ma.noba.identity.domain.event.OtpInitiatedEvent;
import ma.noba.identity.domain.event.OtpVerifiedEvent;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import ma.noba.provider.domain.event.ProviderCreatedEvent;
import ma.noba.provider.domain.event.ProviderRatedEvent;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.event.ProviderUpdatedEvent;
import ma.noba.qa.domain.event.QuestionAskedEvent;
import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.event.ClientServiceCompletedEvent;
import ma.noba.queue.domain.event.ClientTurnStartedEvent;
import ma.noba.shared.infra.event.EventIdempotencyStore;
import ma.noba.shared.domain.event.AccountDeletionRequested;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.UserOnboardingCompletedEvent;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "event.bus.rabbitmq.enabled", havingValue = "true")
public class RabbitMqEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventConsumer.class);

    private static final Map<String, Class<? extends DomainEvent>> EVENT_TYPE_MAP = Map.ofEntries(
            Map.entry("user.signed_up", UserSignedUpEvent.class),
            Map.entry("user.authenticated", UserAuthenticatedEvent.class),
            Map.entry("user.onboarding_completed", UserOnboardingCompletedEvent.class),
            Map.entry("user.status_changed", UserStatusChangedEvent.class),
            Map.entry("account.deletion_requested", AccountDeletionRequested.class),
            Map.entry("otp.initiated", OtpInitiatedEvent.class),
            Map.entry("otp.verified", OtpVerifiedEvent.class),
            Map.entry("otp.failed", OtpFailedEvent.class),
            Map.entry("queue.client.joined", ClientJoinedQueueEvent.class),
            Map.entry("queue.client.left", ClientLeftQueueEvent.class),
            Map.entry("queue.client.turn.started", ClientTurnStartedEvent.class),
            Map.entry("queue.client.service.completed", ClientServiceCompletedEvent.class),
            Map.entry("provider.created", ProviderCreatedEvent.class),
            Map.entry("provider.updated", ProviderUpdatedEvent.class),
            Map.entry("provider.rated", ProviderRatedEvent.class),
            Map.entry("provider.status_changed", ProviderStatusChangedEvent.class),
            Map.entry("qa.question.asked", QuestionAskedEvent.class)
    );

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final EventIdempotencyStore idempotencyStore;

    public RabbitMqEventConsumer(
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            RabbitTemplate rabbitTemplate,
            EventIdempotencyStore idempotencyStore) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.rabbitTemplate = rabbitTemplate;
        this.idempotencyStore = idempotencyStore;
    }

    @RabbitListener(queues = "${event.rabbitmq.queue:noba-events-queue}")
    public void onMessage(String message) {
        String eventId = extractEventId(message);

        if (eventId != null && (idempotencyStore.isProcessed(eventId) || idempotencyStore.isGloballyProcessed(eventId))) {
            log.debug("Skipping duplicate event: {}", eventId);
            return;
        }

        try {
            JsonNode tree = objectMapper.readTree(message);
            String eventType = tree.get("eventType").asText();
            Class<? extends DomainEvent> eventClass = EVENT_TYPE_MAP.get(eventType);
            if (eventClass == null) {
                log.warn("Unknown event type: {}", eventType);
                return;
            }

            DomainEvent typedEvent = objectMapper.treeToValue(tree, eventClass);
            eventPublisher.publishEvent(typedEvent);

            if (eventId != null) {
                idempotencyStore.markProcessed(eventId);
            }
            log.debug("Processed event {} successfully", eventType);
        } catch (Exception e) {
            log.error("Failed to process event, will retry via framework: {}", eventTypeFromMessage(message), e);
            throw new RuntimeException("Event processing failed, will retry", e);
        }
    }

    private String eventTypeFromMessage(String message) {
        try {
            return objectMapper.readTree(message).get("eventType").asText();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractEventId(String payload) {
        try {
            JsonNode tree = objectMapper.readTree(payload);
            JsonNode idNode = tree.get("eventId");
            return idNode != null ? idNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
