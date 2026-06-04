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
public class EmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);
    private final EmailSender emailSender;
    private final OtpTemplateEngine otpTemplateEngine;

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void sendOtp(String destination, String code, Locale locale) {
        Map<String, Object> variables = Map.of("code", code);
        String body = otpTemplateEngine.render("otp-email", variables, locale);
        emailSender.send(destination, "Your Noba verification code", body);
    }
}
