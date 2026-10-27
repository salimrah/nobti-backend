package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.OtpChannel;

import java.util.Locale;

public interface OtpSender {
    OtpChannel channel();
    void sendOtp(String destination, String code, Locale locale);
}
