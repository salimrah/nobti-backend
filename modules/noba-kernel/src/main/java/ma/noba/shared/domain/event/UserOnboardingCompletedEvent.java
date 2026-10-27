package ma.noba.shared.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UserOnboardingCompletedEvent extends BaseDomainEvent {
    private final UUID userId;

    @JsonCreator
    public UserOnboardingCompletedEvent(@JsonProperty("userId") UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }

    @Override
    public String eventType() { return "user.onboarding_completed"; }
}
