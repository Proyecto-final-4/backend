package com.backend.backend.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM symmetric encryption service for data at rest. Also provides deterministic
 * HMAC-SHA256 for fields that require equality lookups (e.g. email).
 *
 * <p>Stored format: {@code enc:v1:<Base64(IV || ciphertext || tag)>}
 *
 * <p>The {@code enc:v1:} prefix makes it possible to detect whether a value is already encrypted,
 * which simplifies migration of existing plaintext data.
 */
@Service
public final class EncryptionService {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static final String PREFIX = "enc:v1:";

    private final byte[] keyBytes;
    private final SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
        this.keyBytes = Base64.getDecoder().decode(base64Key);
        if (this.keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "APP_ENCRYPTION_KEY must be 256 bits (32 bytes encoded as Base64).");
        }
        this.secretKey = new SecretKeySpec(this.keyBytes, "AES");
    }

    /**
     * Encrypts plaintext with AES-256-GCM using a random IV per value.
     *
     * @param plaintext text to encrypt; returns {@code null} if {@code null}
     * @return string with {@code enc:v1:} prefix followed by the Base64 payload
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt value.", e);
        }
    }

    /**
     * Decrypts a value previously encrypted with {@link #encrypt(String)}.
     *
     * <p>If the value does not start with the {@code enc:v1:} prefix it is assumed to be plaintext
     * (migration of existing data) and is returned as-is.
     *
     * @param encryptedData encrypted or plaintext value; returns {@code null} if {@code null}
     * @return decrypted plaintext
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        if (!encryptedData.startsWith(PREFIX)) {
            return encryptedData;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to decrypt value.", e);
        }
    }

    /**
     * Computes HMAC-SHA256 of normalized data (lowercase + trim). Used for fields with a UNIQUE
     * constraint that require deterministic database lookups.
     *
     * @param data text to sign; returns {@code null} if {@code null}
     * @return Base64-encoded HMAC (44 chars)
     */
    public String hmac(String data) {
        if (data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(keyBytes, HMAC_SHA256));
            byte[] result =
                    mac.doFinal(
                            data.toLowerCase(Locale.ROOT).trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to compute HMAC.", e);
        }
    }
}
