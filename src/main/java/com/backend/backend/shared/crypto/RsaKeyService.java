package com.backend.backend.shared.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RSA-2048 service for asymmetric encryption of login and registration payloads.
 *
 * <p>Generates an ephemeral key pair when the application starts. The public key is exposed at
 * {@code GET /auth/public-key} (Base64 SPKI). The frontend uses it with the Web Crypto API
 * (RSA-OAEP + SHA-256) to encrypt credentials before sending them.
 *
 * <p>Keys are volatile: they are regenerated on every restart, so the frontend must always fetch
 * the public key before each login.
 */
@Service
public final class RsaKeyService {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyService.class);
    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private final KeyPair keyPair;

    public RsaKeyService() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE);
            this.keyPair = generator.generateKeyPair();
            log.info("RSA-{} key pair generated successfully for credential encryption.", KEY_SIZE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair.", e);
        }
    }

    /**
     * Returns the public key as standard Base64-encoded SPKI (no line breaks). Compatible with
     * {@code SubtleCrypto.importKey("spki", ...)} from the Web Crypto API.
     */
    public String getPublicKeyBase64() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        return Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    /**
     * Decrypts a value encrypted by the client with RSA-OAEP + SHA-256 (MGF1/SHA-256).
     *
     * @param base64Ciphertext Base64 payload received from the client
     * @return decrypted plaintext
     */
    public String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isBlank()) {
            throw new IllegalArgumentException("Encrypted value cannot be empty.");
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64Ciphertext);

            OAEPParameterSpec oaepSpec =
                    new OAEPParameterSpec(
                            "SHA-256",
                            "MGF1",
                            new MGF1ParameterSpec("SHA-256"),
                            PSource.PSpecified.DEFAULT);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), oaepSpec);

            return new String(cipher.doFinal(cipherBytes), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to decrypt RSA credential.", e);
        }
    }
}
