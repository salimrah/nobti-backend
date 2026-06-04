package ma.noba.identity.domain.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.exception.UnsupportedChannelException;
import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.shared.DomainService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@DomainService
@RequiredArgsConstructor
public class OtpSenderRegistry {

    private final List<OtpSender> otpSenders;
    private final Map<OtpChannel, OtpSender> otpSenderByChannel = new EnumMap<>(OtpChannel.class);

    @PostConstruct
    public void init() {
        for (OtpSender sender : otpSenders) {
            otpSenderByChannel.put(sender.channel(), sender);
        }
    }

    public OtpSender get(OtpChannel channel) {
        if(!otpSenderByChannel.containsKey(channel)) {
            throw new UnsupportedChannelException("No sender for channel: " + channel);
        }
        return otpSenderByChannel.get(channel);
    }

}

