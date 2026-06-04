package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.OtpCodeGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@Primary
public class SecureRandomOtpCodeGenerator implements OtpCodeGenerator<String> {

    private static final SecureRandom rnd = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    @Override
    public String generate(int digits) {
        if (digits <= 0) throw new IllegalArgumentException("Digits must be positive");

        char[] otp = new char[digits];
        for (int i = 0; i < digits; i++) {
            otp[i] = (char) ('0' + rnd.nextInt(10));
        }
        return new String(otp);
    }

    @Override
    public String generate() {
        return generate(CODE_LENGTH);
    }

//    private String generateCode(int digits) {
//        int min = (int) Math.pow(10, digits - 1);
//        int max = (int) Math.pow(10, digits) - 1;
//        return String.valueOf(rnd.nextInt(max - min + 1) + min);
//    }




}
