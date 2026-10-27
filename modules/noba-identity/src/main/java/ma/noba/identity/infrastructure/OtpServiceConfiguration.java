package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.model.OtpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OtpServiceConfiguration {

    @Value("${otp.ttl-minutes:5}") int ttlMinutes;
    @Value("${otp.max-attempts:5}") int maxAttempts;
    @Value("${otp.rate-limits.send-per-target-per-hour:10}") int sendPerTargetPerHour;
    @Value("${otp.rate-limits.verify-per-target-per-hour:30}") int verifyPerTargetPerHour;
    @Value("${otp.default-length:6}") int defaultOtpLength;

    @Bean
    public OtpConfig otpConfig() {
        return new OtpConfig(
                maxAttempts,
                Duration.ofMinutes(ttlMinutes),
                sendPerTargetPerHour,
                verifyPerTargetPerHour,
                defaultOtpLength
        );
    }
}

