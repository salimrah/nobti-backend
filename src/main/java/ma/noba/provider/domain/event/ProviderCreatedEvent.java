package ma.noba.provider.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ProviderCreatedEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID userId;

    @JsonCreator
    public ProviderCreatedEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("userId") UUID userId) {
        this.providerId = providerId;
        this.userId = userId;
    }

    @Override
    public String eventType() { return "provider.created"; }

    public UUID providerId() {
        return providerId;
    }

    public UUID userId() {
        return userId;
    }
}
