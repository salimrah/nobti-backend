package ma.noba.identity.infrastructure.crypto;


import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for PII fields (email, phone).
 *
 * Key rotation strategy:
 * - A single key is loaded at startup from {@code security.pii.secret-b64}.
 * - To rotate: deploy a new key, run a one-off migration to re-encrypt all
 *   existing values, then remove the old key from configuration.
 * - Future improvement: adopt a key-version prefix scheme so old values
 *   remain decryptable during rotation windows.
 */
public class AesGcmCrypto {
    private static SecretKeySpec KEY;
    private static final SecureRandom RAND = new SecureRandom();

    public static void initFromBase64(String b64) {
        byte[] key = Base64.getDecoder().decode(b64);
        if (key.length != 32) throw new IllegalArgumentException("PII key must be 32 bytes");
        KEY = new SecretKeySpec(key, "AES");
    }

    public static String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[12];
            RAND.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String decrypt(String cipherB64) {
        if (cipherB64 == null) return null;
        try {
            byte[] data = Base64.getDecoder().decode(cipherB64);
            byte[] iv = java.util.Arrays.copyOfRange(data, 0, 12);
            byte[] ct = java.util.Arrays.copyOfRange(data, 12, data.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ct), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
