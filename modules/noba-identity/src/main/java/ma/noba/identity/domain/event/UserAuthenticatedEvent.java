package ma.noba.identity.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.event.BaseDomainEvent;

public class UserAuthenticatedEvent extends BaseDomainEvent {
    private final String userId;
    private final String method;

    @JsonCreator
    public UserAuthenticatedEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("method") String method) {
        this.userId = userId;
        this.method = method;
    }

    public String getUserId() { return userId; }
    public String getMethod() { return method; }

    @Override
    public String eventType() { return "user.authenticated"; }
}
