package ma.noba.queue.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ClientServiceCompletedEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID clientId;
    private final UUID queueId;
    private final String serviceId;

    @JsonCreator
    public ClientServiceCompletedEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("queueId") UUID queueId,
            @JsonProperty("serviceId") String serviceId) {
        this.providerId = providerId;
        this.clientId = clientId;
        this.queueId = queueId;
        this.serviceId = serviceId;
    }

    public UUID providerId() { return providerId; }
    public UUID clientId() { return clientId; }
    public UUID queueId() { return queueId; }
    public String serviceId() { return serviceId; }

    @Override
    public String eventType() { return "queue.client.service.completed"; }
}
