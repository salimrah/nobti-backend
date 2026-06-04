package ma.noba.identity.infrastructure.crypto;

import ma.noba.identity.domain.service.OtpCodeEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptOtpCodeEncoder implements OtpCodeEncoder {

    private final BCryptPasswordEncoder otpEncoder = new BCryptPasswordEncoder();

    @Override
    public String encode(String raw) {
        return otpEncoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        return otpEncoder.matches(raw, hash);
    }
}
