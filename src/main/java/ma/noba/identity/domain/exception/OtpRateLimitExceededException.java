package ma.noba.identity.domain.exception;

public class OtpRateLimitExceededException extends RuntimeException {
    public OtpRateLimitExceededException(String phoneNumber) {
        super("Too many OTP requests for phone number: " + phoneNumber);
    }
}
