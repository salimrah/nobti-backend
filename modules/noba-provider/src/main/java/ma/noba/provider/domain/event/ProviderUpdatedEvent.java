package ma.noba.provider.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ProviderUpdatedEvent extends BaseDomainEvent {

    private final UUID providerId;

    @JsonCreator
    public ProviderUpdatedEvent(@JsonProperty("providerId") UUID providerId) {
        this.providerId = providerId;
    }

    @Override
    public String eventType() { return "provider.updated"; }

    public UUID providerId() {
        return providerId;
    }
}
