package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.UserTotpSecret;
import ma.noba.identity.domain.repository.UserTotpSecretRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * RFC 6238 time-based one-time passwords. Secrets are Base32 (RFC 4648) so they can be typed into
 * any authenticator app; validation accepts the current step plus one step of clock drift and
 * records the last accepted step to stop replays.
 */
@Service
@RequiredArgsConstructor
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTotpSecretRepository secrets;

    @Value("${security.mfa.issuer:Noba}")
    private String issuer;

    public record Setup(String secret, String otpauthUri) {
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return secrets.findById(userId).map(UserTotpSecret::isEnabled).orElse(false);
    }

    /** Generates (or replaces an unconfirmed) secret and returns the enrolment payload. */
    @Transactional
    public Setup startSetup(UUID userId, String accountLabel) {
        UserTotpSecret record = secrets.findById(userId).orElseGet(UserTotpSecret::new);
        if (record.isEnabled()) {
            throw new IllegalArgumentException("Two-factor authentication is already enabled");
        }
        String secret = generateSecret();
        record.setUserId(userId);
        record.setSecret(secret);
        record.setEnabled(false);
        record.setConfirmedAt(null);
        record.setLastUsedStep(null);
        secrets.save(record);
        return new Setup(secret, otpauthUri(accountLabel, secret));
    }

    /** Confirms enrolment with a first code. */
    @Transactional
    public void enable(UUID userId, String code) {
        UserTotpSecret record = secrets.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Start the two-factor setup first"));
        if (record.isEnabled()) {
            throw new IllegalArgumentException("Two-factor authentication is already enabled");
        }
        long step = matchingStep(record.getSecret(), code);
        if (step < 0) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        record.setEnabled(true);
        record.setConfirmedAt(Instant.now());
        record.setLastUsedStep(step);
        secrets.save(record);
    }

    @Transactional
    public void disable(UUID userId, String code) {
        UserTotpSecret record = secrets.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Two-factor authentication is not enabled"));
        if (record.isEnabled() && matchingStep(record.getSecret(), code) < 0) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        secrets.delete(record);
    }

    /** Verifies a login code, rejecting replays of an already-used time step. */
    @Transactional
    public boolean verify(UUID userId, String code) {
        UserTotpSecret record = secrets.findById(userId).orElse(null);
        if (record == null || !record.isEnabled()) {
            return false;
        }
        long step = matchingStep(record.getSecret(), code);
        if (step < 0 || (record.getLastUsedStep() != null && step <= record.getLastUsedStep())) {
            return false;
        }
        record.setLastUsedStep(step);
        secrets.save(record);
        return true;
    }

    public String otpauthUri(String accountLabel, String secret) {
        String label = (issuer + ":" + (accountLabel == null ? "account" : accountLabel)).trim();
        return "otpauth://totp/" + urlEncode(label)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    private long matchingStep(String secret, String code) {
        if (code == null) {
            return -1;
        }
        String normalized = code.replaceAll("\\s", "");
        byte[] key = base32Decode(secret);
        long current = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (long step = current - 1; step <= current + 1; step++) {
            if (MessageDigest.isEqual(generate(key, step).getBytes(StandardCharsets.US_ASCII),
                    normalized.getBytes(StandardCharsets.US_ASCII))) {
                return step;
            }
        }
        return -1;
    }

    private static String generate(byte[] key, long step) {
        try {
            byte[] counter = ByteBuffer.allocate(8).putLong(step).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute TOTP", e);
        }
    }

    private static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return out.toString();
    }

    static byte[] base32Decode(String value) {
        String normalized = value == null ? "" : value.replace("=", "")
                .replace(" ", "").toUpperCase(Locale.ROOT);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : normalized.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base32 secret");
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
