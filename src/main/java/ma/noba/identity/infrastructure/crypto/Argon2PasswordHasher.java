package ma.noba.identity.infrastructure.crypto;

import ma.noba.identity.domain.service.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}