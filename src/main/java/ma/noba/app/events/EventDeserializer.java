package ma.noba.app.events;

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
import ma.noba.shared.domain.event.AccountDeletionRequested;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.UserOnboardingCompletedEvent;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.event.UserStatusChangedEvent;
import ma.noba.shared.domain.model.OutboxEvent;

import java.util.Map;
import java.util.function.Function;

public class EventDeserializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Function<String, DomainEvent>> deserializers;

    public EventDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.deserializers = Map.ofEntries(
                Map.entry("user.signed_up", json -> deserialize(json, UserSignedUpEvent.class)),
                Map.entry("user.authenticated", json -> deserialize(json, UserAuthenticatedEvent.class)),
                Map.entry("user.onboarding_completed", json -> deserialize(json, UserOnboardingCompletedEvent.class)),
                Map.entry("user.status_changed", json -> deserialize(json, UserStatusChangedEvent.class)),
                Map.entry("account.deletion_requested", json -> deserialize(json, AccountDeletionRequested.class)),
                Map.entry("otp.initiated", json -> deserialize(json, OtpInitiatedEvent.class)),
                Map.entry("otp.verified", json -> deserialize(json, OtpVerifiedEvent.class)),
                Map.entry("otp.failed", json -> deserialize(json, OtpFailedEvent.class)),
                Map.entry("queue.client.joined", json -> deserialize(json, ClientJoinedQueueEvent.class)),
                Map.entry("queue.client.left", json -> deserialize(json, ClientLeftQueueEvent.class)),
                Map.entry("queue.client.turn.started", json -> deserialize(json, ClientTurnStartedEvent.class)),
                Map.entry("queue.client.service.completed", json -> deserialize(json, ClientServiceCompletedEvent.class)),
                Map.entry("provider.created", json -> deserialize(json, ProviderCreatedEvent.class)),
                Map.entry("provider.updated", json -> deserialize(json, ProviderUpdatedEvent.class)),
                Map.entry("provider.rated", json -> deserialize(json, ProviderRatedEvent.class)),
                Map.entry("provider.status_changed", json -> deserialize(json, ProviderStatusChangedEvent.class)),
                Map.entry("qa.question.asked", json -> deserialize(json, QuestionAskedEvent.class))
        );
    }

    public DomainEvent deserialize(OutboxEvent outboxEvent) {
        Function<String, DomainEvent> deserializer = deserializers.get(outboxEvent.getEventType());
        if (deserializer == null) {
            throw new IllegalArgumentException("Unknown event type: " + outboxEvent.getEventType());
        }
        return deserializer.apply(outboxEvent.getPayload());
    }

    private <T extends DomainEvent> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + json, e);
        }
    }
}
