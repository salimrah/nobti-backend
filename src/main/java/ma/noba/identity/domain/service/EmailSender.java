package ma.noba.identity.domain.service;

public interface EmailSender {
    void send(String to, String subject, String body);
}
