package ma.noba.provider.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ProviderRatedEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID customerId;
    private final int rating;

    @JsonCreator
    public ProviderRatedEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("customerId") UUID customerId,
            @JsonProperty("rating") int rating) {
        this.providerId = providerId;
        this.customerId = customerId;
        this.rating = rating;
    }

    @Override
    public String eventType() { return "provider.rated"; }

    public UUID providerId() {
        return providerId;
    }

    public UUID customerId() {
        return customerId;
    }

    public int rating() {
        return rating;
    }
}
