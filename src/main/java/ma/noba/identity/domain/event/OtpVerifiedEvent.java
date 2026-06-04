package ma.noba.identity.domain.event;

import ma.noba.shared.domain.event.BaseDomainEvent;

public class OtpVerifiedEvent extends BaseDomainEvent {
    private final String channel;

    public OtpVerifiedEvent(String channel) {
        this.channel = channel;
    }

    public String getChannel() { return channel; }

    @Override
    public String eventType() { return "otp.verified"; }
}
