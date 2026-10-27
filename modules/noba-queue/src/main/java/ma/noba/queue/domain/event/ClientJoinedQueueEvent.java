package ma.noba.queue.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ClientJoinedQueueEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID clientId;
    private final UUID queueId;
    private final int position;
    private final Instant estimatedStartTime;

    @JsonCreator
    public ClientJoinedQueueEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("clientId") UUID clientId,
            @JsonProperty("queueId") UUID queueId,
            @JsonProperty("position") int position,
            @JsonProperty("estimatedStartTime") Instant estimatedStartTime) {
        this.providerId = providerId;
        this.clientId = clientId;
        this.queueId = queueId;
        this.position = position;
        this.estimatedStartTime = estimatedStartTime;
    }

    public UUID providerId() { return providerId; }
    public UUID clientId() { return clientId; }
    public UUID queueId() { return queueId; }
    public int position() { return position; }
    public Instant estimatedStartTime() { return estimatedStartTime; }

    @Override
    public String eventType() { return "queue.client.joined"; }
}
