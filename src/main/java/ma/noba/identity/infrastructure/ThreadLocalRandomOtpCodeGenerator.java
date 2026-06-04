package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.OtpCodeGenerator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ThreadLocalRandomOtpCodeGenerator implements OtpCodeGenerator<String> {

    private static final int CODE_LENGTH = 6;
    private static final int MAX_OTP_VALUE = (int) Math.pow(10, CODE_LENGTH) - 1;

    @Override
    public String generate(int digits) {
        if (digits <= 0) throw new IllegalArgumentException("Digits must be positive");
        int maxValue = (int) Math.pow(10, digits) - 1;
        return String.format("%0" + digits + "d",
                ThreadLocalRandom.current().nextInt(maxValue + 1));
    }

    @Override
    public String generate() {
        return generate(CODE_LENGTH);
    }

}
