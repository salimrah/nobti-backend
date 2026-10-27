package ma.noba.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final Integer aggregateVersion;
    private final String correlationId;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.aggregateVersion = null;
        this.correlationId = null;
    }

    protected BaseDomainEvent(Integer aggregateVersion, String correlationId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.aggregateVersion = aggregateVersion;
        this.correlationId = correlationId;
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    public Integer getAggregateVersion() {
        return aggregateVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
