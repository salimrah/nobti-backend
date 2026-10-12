package ma.noba.identity.domain.service;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.event.OtpFailedEvent;
import ma.noba.identity.domain.event.OtpInitiatedEvent;
import ma.noba.identity.domain.event.OtpVerifiedEvent;
import ma.noba.identity.domain.exception.AttemptsExhaustedException;
import ma.noba.identity.domain.exception.ChallengeNotFoundException;
import ma.noba.identity.domain.exception.InvalidCodeException;
import ma.noba.identity.domain.exception.TooManyRequestsException;
import ma.noba.identity.domain.model.OtpChallenge;
import ma.noba.identity.domain.model.OtpChallengeStatus;
import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.identity.domain.model.OtpConfig;
import ma.noba.identity.domain.repository.OtpChallengeRepository;
import ma.noba.shared.DomainService;
import ma.noba.shared.IdempotencyCache;
import ma.noba.shared.domain.event.EventBus;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@DomainService
@RequiredArgsConstructor
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpChallengeRepository repository;
    private final RateLimiterService rateLimiter;
    private final MeterRegistry meterRegistry;
    private final OtpSenderRegistry otpSenderRegistry;
    private final OtpCodeEncoder otpEncoder ;
    private final OtpCodeGenerator<String> otpCodeGenerator;
    private final OtpTargetNormalizer otpTargetNormalizer;
    private final OtpConfig otpConfig;
    private final IdempotencyCache idempotencyCache;
    private final EventBus eventBus;

    public String startChallenge(String targetRaw, OtpChannel channel, String purpose, Locale locale, String idempotencyKey) {
        String target = otpTargetNormalizer.normalize(targetRaw, channel);
        // The rate-limit key must NOT include the caller-supplied purpose, otherwise an attacker
        // could rotate purpose values to bypass the per-target send limit.
        String rateKey = "rl:otp:send:" + channel + ":" + target;
        if (!rateLimiter.tryConsume(rateKey, 1)) throw new TooManyRequestsException("Rate limit");

        String idemKey = idempotencyKey != null ? DigestUtils.sha256Hex(idempotencyKey) : null;
        if (idemKey != null) {
            String existing = idempotencyCache.putIfAbsent("idem:otp:start:" + idemKey, "IN_USE",60);
            if (!"IN_USE".equals(existing)) return existing;
        }

        String code = otpCodeGenerator.generate(otpConfig.defaultOtpLength());
        var challenge = OtpChallenge.builder()
                .id(UUID.randomUUID().toString())
                .target(target)
                .channel(channel)
                .purpose(purpose)
                .codeHash(otpEncoder.encode(code))
                .attempts(0)
                .maxAttempts(otpConfig.maxAttempts())
                .status(OtpChallengeStatus.PENDING)
                .build();

        repository.save(challenge, otpConfig.ttl());
        try {
            otpSenderRegistry.get(channel).sendOtp(target, code, locale);
        } catch (Exception e) {
            repository.delete(challenge.getId());
            if (idemKey != null) {
                idempotencyCache.set("idem:otp:start:" + idemKey, "FAILED", 60);
            }
            throw e;
        }
        meterRegistry.counter("otp.sent", "channel", channel.name(), "purpose", purpose).increment();
        eventBus.publish(new OtpInitiatedEvent(channel.name(), purpose));
        log.info("OTP challenge started: id={}, channel={}, purpose={}", challenge.getId(), channel, purpose);
        if (idemKey != null) {
            idempotencyCache.set("idem:otp:start:" + idemKey, challenge.getId(), 300);
        }
        return challenge.getId();
    }

    public OtpVerificationResult verify(String challengeId, String code) {
        var challenge = repository.find(challengeId).orElseThrow(() -> new ChallengeNotFoundException("Challenge not found"));

        String rateKey = "rl:otp:verify:" + challenge.getChannel() + ":" + challenge.getTarget();
        if (!rateLimiter.tryConsume(rateKey, 1)) throw new TooManyRequestsException("Rate limit");

        if (challenge.getStatus() != OtpChallengeStatus.PENDING) throw new InvalidCodeException("Challenge is not pending");

        // Atomic increment: concurrent guesses must not each read the same stale attempt count.
        int attempts = repository.incrementAttempts(challenge.getId(), otpConfig.ttl());
        if (attempts > challenge.getMaxAttempts()) throw new AttemptsExhaustedException("Too many attempts");

        boolean ok = otpEncoder.matches(code, challenge.getCodeHash());
        if (!ok) {
            int remaining = Math.max(0, challenge.getMaxAttempts() - attempts);
            meterRegistry.counter("otp.verify.fail", "channel", challenge.getChannel().name()).increment();
            eventBus.publish(new OtpFailedEvent("Invalid code"));
            String msg = remaining > 0
                    ? "Invalid code — " + remaining + " attempt" + (remaining == 1 ? "" : "s") + " remaining"
                    : "Invalid code — no attempts remaining";
            throw new InvalidCodeException(msg);
        }
        String target = challenge.getTarget();
        if (repository.delete(challenge.getId())) {
            meterRegistry.counter("otp.verify.success", "channel", challenge.getChannel().name()).increment();
            eventBus.publish(new OtpVerifiedEvent(challenge.getChannel().name()));
        }
        return new OtpVerificationResult(target, challenge.getChannel());
    }

}