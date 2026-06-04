package ma.noba.identity.domain.event;

import ma.noba.shared.domain.event.BaseDomainEvent;

public class OtpInitiatedEvent extends BaseDomainEvent {
    private final String channel;
    private final String purpose;

    public OtpInitiatedEvent(String channel, String purpose) {
        this.channel = channel;
        this.purpose = purpose;
    }

    public String getChannel() { return channel; }
    public String getPurpose() { return purpose; }

    @Override
    public String eventType() { return "otp.initiated"; }
}
