package ma.noba.identity.domain.service;

public interface OtpCodeEncoder {
    String encode(String raw);
    boolean matches(String raw, String hash);
}
