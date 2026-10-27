package ma.noba.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final MeterRegistry registry;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordLoginSuccess(String method) {
        Counter.builder("auth.login.success")
                .tag("method", method)
                .tag("service", "noba-backend")
                .description("Successful login attempts")
                .register(registry)
                .increment();
    }

    public void recordLoginFailure(String method, String reason) {
        Counter.builder("auth.login.failure")
                .tag("method", method)
                .tag("reason", reason)
                .tag("service", "noba-backend")
                .description("Failed login attempts")
                .register(registry)
                .increment();
    }

    public void recordSignup(String method) {
        Counter.builder("auth.signup.total")
                .tag("method", method)
                .tag("service", "noba-backend")
                .description("User signups")
                .register(registry)
                .increment();
    }

    public void recordOtpSent(String channel) {
        Counter.builder("auth.otp.sent")
                .tag("channel", channel)
                .tag("service", "noba-backend")
                .description("OTP codes sent")
                .register(registry)
                .increment();
    }

    public void recordOtpVerifySuccess() {
        Counter.builder("auth.otp.verify.success")
                .tag("service", "noba-backend")
                .description("Successful OTP verifications")
                .register(registry)
                .increment();
    }

    public void recordOtpVerifyFailure(String reason) {
        Counter.builder("auth.otp.verify.failure")
                .tag("reason", reason)
                .tag("service", "noba-backend")
                .description("Failed OTP verifications")
                .register(registry)
                .increment();
    }

    public Timer.Sample startLoginTimer() {
        return Timer.start(registry);
    }

    public void stopLoginTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("auth.login.duration")
                .tag("service", "noba-backend")
                .description("Login operation duration")
                .register(registry));
    }

    public Timer.Sample startSignupTimer() {
        return Timer.start(registry);
    }

    public void stopSignupTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("auth.signup.duration")
                .tag("service", "noba-backend")
                .description("Signup operation duration")
                .register(registry));
    }
}
