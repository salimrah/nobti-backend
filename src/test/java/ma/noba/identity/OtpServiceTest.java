package ma.noba.identity;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
import ma.noba.identity.domain.service.*;
import ma.noba.shared.IdempotencyCache;
import ma.noba.shared.domain.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock OtpChallengeRepository repository;
    @Mock RateLimiterService rateLimiter;
    @Mock MeterRegistry meterRegistry;
    @Mock OtpSenderRegistry otpSenderRegistry;
    @Mock OtpCodeEncoder otpEncoder;
    @Mock OtpCodeGenerator<String> otpCodeGenerator;
    @Mock OtpTargetNormalizer otpTargetNormalizer;
    @Mock OtpConfig otpConfig;
    @Mock IdempotencyCache idempotencyCache;
    @Mock EventBus eventBus;
    @Mock OtpSender otpSender;
    @Mock Counter counter;

    private OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(repository, rateLimiter, meterRegistry, otpSenderRegistry,
                otpEncoder, otpCodeGenerator, otpTargetNormalizer, otpConfig,
                idempotencyCache, eventBus);
    }

    @Test
    @DisplayName("Start challenge success — normalizes target, saves, sends OTP")
    void startChallenge_success() {
        var target = "+212600000000";
        var normalized = "+212600000001";
        var code = "123456";
        var challengeId = "challenge-1";

        when(otpTargetNormalizer.normalize(target, OtpChannel.SMS)).thenReturn(normalized);
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);
        when(otpCodeGenerator.generate(anyInt())).thenReturn(code);
        when(otpEncoder.encode(code)).thenReturn("encoded");
        when(otpConfig.defaultOtpLength()).thenReturn(6);
        when(otpConfig.maxAttempts()).thenReturn(5);
        when(otpConfig.ttl()).thenReturn(Duration.ofMinutes(5));
        when(otpSenderRegistry.get(OtpChannel.SMS)).thenReturn(otpSender);
        when(meterRegistry.counter(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(counter);

        var result = service.startChallenge(target, OtpChannel.SMS, "LOGIN", Locale.ENGLISH, null);

        assertThat(result).isNotNull();
        verify(repository).save(any(OtpChallenge.class), any(Duration.class));
        verify(otpSender).sendOtp(normalized, code, Locale.ENGLISH);
        verify(eventBus).publish(any(OtpInitiatedEvent.class));
    }

    @Test
    @DisplayName("Start challenge rate limited — throws TooManyRequestsException")
    void startChallenge_rateLimited() {
        when(otpTargetNormalizer.normalize(anyString(), any())).thenReturn("normalized");
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(false);

        assertThatThrownBy(() -> service.startChallenge("+212600000000", OtpChannel.SMS, "LOGIN", Locale.ENGLISH, null))
                .isInstanceOf(TooManyRequestsException.class);

        verify(repository, never()).save(any(), any());
    }

    @Test
    @DisplayName("Start challenge with idempotency key — returns existing challenge ID")
    void startChallenge_idempotent() {
        var target = "+212600000000";
        var idemKey = "idem-123";
        var idemHash = "sha256-of-idem-123";

        when(otpTargetNormalizer.normalize(target, OtpChannel.SMS)).thenReturn(target);
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);
        when(idempotencyCache.putIfAbsent(eq("idem:otp:start:" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(idemKey)), eq("IN_USE"), eq(60)))
                .thenReturn("existing-challenge-id");

        var result = service.startChallenge(target, OtpChannel.SMS, "LOGIN", Locale.ENGLISH, idemKey);

        assertThat(result).isEqualTo("existing-challenge-id");
        verify(repository, never()).save(any(), any());
    }

    @Test
    @DisplayName("Verify success — returns target and channel")
    void verify_success() {
        var challengeId = "challenge-1";
        var code = "123456";
        var target = "+212600000000";
        var challenge = OtpChallenge.builder()
                .id(challengeId)
                .target(target)
                .channel(OtpChannel.SMS)
                .purpose("LOGIN")
                .codeHash("encoded")
                .attempts(0)
                .maxAttempts(5)
                .status(OtpChallengeStatus.PENDING)
                .build();

        when(repository.find(challengeId)).thenReturn(Optional.of(challenge));
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);
        when(otpEncoder.matches(code, "encoded")).thenReturn(true);
        when(repository.delete(challengeId)).thenReturn(true);
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        var result = service.verify(challengeId, code);

        assertThat(result.target()).isEqualTo(target);
        assertThat(result.channel()).isEqualTo(OtpChannel.SMS);
        verify(eventBus).publish(any(OtpVerifiedEvent.class));
    }

    @Test
    @DisplayName("Verify invalid code — increments attempts, throws InvalidCodeException")
    void verify_invalidCode() {
        var challengeId = "challenge-1";
        var challenge = OtpChallenge.builder()
                .id(challengeId)
                .target("+212600000000")
                .channel(OtpChannel.SMS)
                .purpose("LOGIN")
                .codeHash("encoded")
                .attempts(0)
                .maxAttempts(5)
                .status(OtpChallengeStatus.PENDING)
                .build();

        when(repository.find(challengeId)).thenReturn(Optional.of(challenge));
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);
        when(otpEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(otpConfig.ttl()).thenReturn(Duration.ofMinutes(5));
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        assertThatThrownBy(() -> service.verify(challengeId, "wrong-code"))
                .isInstanceOf(InvalidCodeException.class);

        assertThat(challenge.getAttempts()).isEqualTo(1);
        verify(eventBus).publish(any(OtpFailedEvent.class));
    }

    @Test
    @DisplayName("Verify max attempts exhausted — throws AttemptsExhaustedException")
    void verify_maxAttemptsExhausted() {
        var challenge = OtpChallenge.builder()
                .id("challenge-1")
                .target("+212600000000")
                .channel(OtpChannel.SMS)
                .purpose("LOGIN")
                .codeHash("encoded")
                .attempts(5)
                .maxAttempts(5)
                .status(OtpChallengeStatus.PENDING)
                .build();

        when(repository.find("challenge-1")).thenReturn(Optional.of(challenge));
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);

        assertThatThrownBy(() -> service.verify("challenge-1", "code"))
                .isInstanceOf(AttemptsExhaustedException.class);
    }

    @Test
    @DisplayName("Verify challenge not pending — throws InvalidCodeException")
    void verify_notPending() {
        var challenge = OtpChallenge.builder()
                .id("challenge-1")
                .target("+212600000000")
                .channel(OtpChannel.SMS)
                .codeHash("encoded")
                .attempts(0)
                .maxAttempts(5)
                .status(OtpChallengeStatus.VERIFIED)
                .build();

        when(repository.find("challenge-1")).thenReturn(Optional.of(challenge));
        when(rateLimiter.tryConsume(anyString(), eq(1L))).thenReturn(true);

        assertThatThrownBy(() -> service.verify("challenge-1", "code"))
                .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    @DisplayName("Verify challenge not found — throws ChallengeNotFoundException")
    void verify_challengeNotFound() {
        when(repository.find("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("unknown", "code"))
                .isInstanceOf(ChallengeNotFoundException.class);
    }
}
