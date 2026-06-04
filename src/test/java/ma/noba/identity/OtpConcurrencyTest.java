package ma.noba.identity;

import io.micrometer.core.instrument.MeterRegistry;
import ma.noba.identity.domain.model.OtpChannel;
import ma.noba.identity.domain.model.OtpChallenge;
import ma.noba.identity.domain.model.OtpChallengeStatus;
import ma.noba.identity.domain.model.OtpConfig;
import ma.noba.identity.domain.repository.OtpChallengeRepository;
import ma.noba.identity.domain.service.*;
import ma.noba.shared.IdempotencyCache;
import ma.noba.shared.domain.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpConcurrencyTest {

    @Mock private OtpChallengeRepository repository;
    @Mock private RateLimiterService rateLimiter;
    @Mock private MeterRegistry meterRegistry;
    @Mock private OtpSenderRegistry otpSenderRegistry;
    @Mock private OtpCodeEncoder otpEncoder;
    @Mock private OtpCodeGenerator<String> otpCodeGenerator;
    @Mock private OtpTargetNormalizer otpTargetNormalizer;
    @Mock private IdempotencyCache idempotencyCache;
    @Mock private EventBus eventBus;

    private OtpService otpService;

    private static final String CHALLENGE_ID = "ch-concurrent-1";
    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(
                repository, rateLimiter, meterRegistry,
                otpSenderRegistry, otpEncoder, otpCodeGenerator,
                otpTargetNormalizer, new OtpConfig(MAX_ATTEMPTS, Duration.ofMinutes(5), 10, 10, 6),
                idempotencyCache, eventBus
        );
    }

    @Test
    @DisplayName("Concurrent verify calls can exceed maxAttempts due to race condition")
    void concurrentVerify_raceCondition() throws Exception {
        OtpChallenge challenge = OtpChallenge.builder()
                .id(CHALLENGE_ID)
                .target("+212600000000")
                .channel(OtpChannel.SMS)
                .purpose("LOGIN")
                .codeHash("hashed-code")
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .status(OtpChallengeStatus.PENDING)
                .build();

        when(repository.find(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
        when(rateLimiter.tryConsume(any(), anyLong())).thenReturn(true);
        when(otpEncoder.matches(any(), any())).thenReturn(false);

        int threadCount = 5;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                try {
                    otpService.verify(CHALLENGE_ID, "wrong-code");
                } catch (Exception ignored) {}
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(challenge.getAttempts()).isGreaterThanOrEqualTo(MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("OTP rate limiter should prevent excessive concurrent sends")
    void concurrentStartChallenge_rateLimited() throws Exception {
        when(otpTargetNormalizer.normalize(any(), any())).thenReturn("+212600000000");

        int threadCount = 10;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int[] results = new int[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                try {
                    otpService.startChallenge("+212600000000", OtpChannel.SMS, "LOGIN", null, null);
                    results[idx] = 0;
                } catch (Exception e) {
                    results[idx] = 1;
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long sent = java.util.Arrays.stream(results).filter(r -> r == 0).count();
        long rejected = java.util.Arrays.stream(results).filter(r -> r == 1).count();
        assertThat(rejected).isGreaterThan(0);
    }
}
