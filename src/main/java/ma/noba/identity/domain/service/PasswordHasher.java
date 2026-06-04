package ma.noba.identity.domain.service;

public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
