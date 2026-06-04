package ma.noba.identity.domain.model;

import java.time.Duration;

public record OtpConfig(
        int maxAttempts,
        Duration ttl,
        int sendPerTargetPerHour,
        int verifyPerTargetPerHour,
        int defaultOtpLength
) {}
