package ma.noba.identity.domain.event;

import ma.noba.shared.domain.event.BaseDomainEvent;

public class OtpFailedEvent extends BaseDomainEvent {
    private final String reason;

    public OtpFailedEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }

    @Override
    public String eventType() { return "otp.failed"; }
}
