package ma.noba.shared.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.model.UserStatus;

import java.util.UUID;

public class UserStatusChangedEvent extends BaseDomainEvent {
    private final UUID userId;
    private final UserStatus oldStatus;
    private final UserStatus newStatus;

    @JsonCreator
    public UserStatusChangedEvent(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("oldStatus") UserStatus oldStatus,
            @JsonProperty("newStatus") UserStatus newStatus) {
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getUserId() { return userId; }
    public UserStatus getOldStatus() { return oldStatus; }
    public UserStatus getNewStatus() { return newStatus; }

    @Override
    public String eventType() { return "user.status_changed"; }
}
