package ma.noba.provider.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ProviderStatusChangedEvent extends BaseDomainEvent {

    private final UUID providerId;
    private final UUID userId;
    private final ProviderStatus oldStatus;
    private final ProviderStatus newStatus;

    @JsonCreator
    public ProviderStatusChangedEvent(
            @JsonProperty("providerId") UUID providerId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("oldStatus") ProviderStatus oldStatus,
            @JsonProperty("newStatus") ProviderStatus newStatus) {
        this.providerId = providerId;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String eventType() { return "provider.status_changed"; }

    public UUID providerId() { return providerId; }
    public UUID userId() { return userId; }
    public ProviderStatus oldStatus() { return oldStatus; }
    public ProviderStatus newStatus() { return newStatus; }
}
