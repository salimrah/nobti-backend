package ma.noba.queue.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ClientLeftQueueEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID clientId;
    private final UUID queueId;

    @JsonCreator
    public ClientLeftQueueEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("queueId") UUID queueId) {
        this.providerId = providerId;
        this.clientId = clientId;
        this.queueId = queueId;
    }

    public UUID providerId() { return providerId; }
    public UUID clientId() { return clientId; }
    public UUID queueId() { return queueId; }

    @Override
    public String eventType() { return "queue.client.left"; }
}
