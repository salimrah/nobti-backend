package ma.noba.shared.infra.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.identity.domain.event.UserAuthenticatedEvent;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.event.ClientTurnStartedEvent;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.model.OutboxEvent;

import java.util.Map;
import java.util.function.Function;

class EventDeserializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Function<String, DomainEvent>> deserializers;

    EventDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.deserializers = Map.of(
                "user.signed_up", json -> deserialize(json, UserSignedUpEvent.class),
                "user.authenticated", json -> deserialize(json, UserAuthenticatedEvent.class),
                "queue.client.joined", json -> deserialize(json, ClientJoinedQueueEvent.class),
                "queue.client.left", json -> deserialize(json, ClientLeftQueueEvent.class),
                "queue.client.turn.started", json -> deserialize(json, ClientTurnStartedEvent.class)
        );
    }

    DomainEvent deserialize(OutboxEvent outboxEvent) {
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
