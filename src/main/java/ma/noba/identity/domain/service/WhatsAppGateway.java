package ma.noba.identity.domain.service;

public interface WhatsAppGateway {
    void sendMessage(String phoneNumber, String messageBody);
}
