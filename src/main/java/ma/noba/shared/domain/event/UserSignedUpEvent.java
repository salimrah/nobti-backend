package ma.noba.shared.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;

public class UserSignedUpEvent extends BaseDomainEvent {
    private final String userId;
    private final String email;
    private final String phone;
    private final UserStatus status;
    private final OnboardingStatus onboardingStatus;

    @JsonCreator
    public UserSignedUpEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("phone") String phone,
            @JsonProperty("status") UserStatus status,
            @JsonProperty("onboardingStatus") OnboardingStatus onboardingStatus) {
        this.userId = userId;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.onboardingStatus = onboardingStatus;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserStatus getStatus() { return status; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }

    @Override
    public String eventType() { return "user.signed_up"; }
}
