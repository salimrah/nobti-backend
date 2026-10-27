package ma.noba.identity.domain.service;

public interface PasswordPolicy {
    void validate(String rawPassword);
}
