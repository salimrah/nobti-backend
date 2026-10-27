package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.shared.DomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

@DomainService
@RequiredArgsConstructor
public class WhatsAppOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppOtpSender.class);
    private final WhatsAppGateway whatsAppGateway;
    private final OtpTemplateEngine otpTemplateEngine;

    @Override
    public OtpChannel channel() {
        return OtpChannel.WHATSAPP;
    }

    @Override
    public void sendOtp(String destination, String code, Locale locale) {
        Map<String, Object> variables = Map.of("code", code);
        String body = otpTemplateEngine.render("otp", variables, locale);
        whatsAppGateway.sendMessage(destination, body);
    }

}

