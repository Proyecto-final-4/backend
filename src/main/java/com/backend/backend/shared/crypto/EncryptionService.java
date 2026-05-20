package com.backend.backend.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de cifrado simétrico AES-256-GCM para datos en reposo. Proporciona además HMAC-SHA256
 * determinista para campos que requieren búsqueda por igualdad (ej. email).
 *
 * <p>Formato almacenado: {@code enc:v1:<Base64(IV || cifrado || tag)>}
 *
 * <p>El prefijo {@code enc:v1:} permite detectar si un valor ya fue cifrado, lo que facilita la
 * migración de datos existentes en texto plano.
 */
@Service
public class EncryptionService {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    static final String PREFIX = "enc:v1:";

    private final byte[] keyBytes;
    private final SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
        this.keyBytes = Base64.getDecoder().decode(base64Key);
        if (this.keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "APP_ENCRYPTION_KEY debe ser de 256 bits (32 bytes codificados en Base64).");
        }
        this.secretKey = new SecretKeySpec(this.keyBytes, "AES");
    }

    /**
     * Cifra un texto plano con AES-256-GCM usando un IV aleatorio por valor.
     *
     * @param plaintext texto a cifrar; si es {@code null} retorna {@code null}
     * @return cadena con prefijo {@code enc:v1:} seguido del payload Base64
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el valor.", e);
        }
    }

    /**
     * Descifra un valor previamente cifrado con {@link #encrypt(String)}.
     *
     * <p>Si el valor no comienza con el prefijo {@code enc:v1:} se asume que es texto plano
     * (migración de datos existentes) y se retorna tal cual.
     *
     * @param encryptedData valor cifrado o texto plano; si es {@code null} retorna {@code null}
     * @return texto plano descifrado
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        if (!encryptedData.startsWith(PREFIX)) {
            return encryptedData;
        }
        try {
            byte[] combined =
                    Base64.getDecoder().decode(encryptedData.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el valor.", e);
        }
    }

    /**
     * Calcula HMAC-SHA256 del dato normalizado (minúsculas + trim). Se usa para campos con
     * restricción UNIQUE que necesitan búsqueda determinista en la base de datos.
     *
     * @param data texto a firmar; si es {@code null} retorna {@code null}
     * @return Base64 del HMAC (44 chars)
     */
    public String hmac(String data) {
        if (data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(keyBytes, HMAC_SHA256));
            byte[] result =
                    mac.doFinal(data.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular HMAC.", e);
        }
    }
}
