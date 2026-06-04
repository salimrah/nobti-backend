package ma.noba.identity.domain.service;

public interface SmsGateway {
    void sendSms(String toPhoneNumber, String messageBody);
}
