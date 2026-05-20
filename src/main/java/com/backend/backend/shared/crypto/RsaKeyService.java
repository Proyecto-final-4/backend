package com.backend.backend.shared.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
 * Servicio RSA-2048 para cifrado asimétrico del payload de login y registro.
 *
 * <p>Genera un par de claves efímero al arrancar la aplicación. La clave pública se expone en
 * {@code GET /auth/public-key} (Base64 SPKI). El frontend la usa con la Web Crypto API
 * (RSA-OAEP + SHA-256) para cifrar las credenciales antes de enviarlas.
 *
 * <p>Las claves son volátiles: se regeneran en cada reinicio, por lo que el frontend siempre debe
 * obtener la clave pública antes de cada login.
 */
@Service
public class RsaKeyService {

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
            log.info("Par de claves RSA-{} generado correctamente para cifrado de credenciales.", KEY_SIZE);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el par de claves RSA.", e);
        }
    }

    /**
     * Retorna la clave pública en formato SPKI codificada en Base64 estándar (sin saltos de línea).
     * Compatible con {@code SubtleCrypto.importKey("spki", ...)} de la Web Crypto API.
     */
    public String getPublicKeyBase64() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        return Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    /**
     * Descifra un valor cifrado por el cliente con RSA-OAEP + SHA-256 (MGF1/SHA-256).
     *
     * @param base64Ciphertext payload Base64 recibido del cliente
     * @return texto plano descifrado
     */
    public String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isBlank()) {
            throw new IllegalArgumentException("El valor cifrado no puede estar vacío.");
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64Ciphertext);

            OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    new MGF1ParameterSpec("SHA-256"),
                    PSource.PSpecified.DEFAULT);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), oaepSpec);

            return new String(cipher.doFinal(cipherBytes), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar credencial RSA.", e);
        }
    }
}
