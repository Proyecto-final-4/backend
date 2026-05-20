package com.backend.backend.shared.crypto;

import com.backend.backend.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migra en caliente los registros de usuarios que fueron creados antes de activar el cifrado:
 *
 * <ul>
 *   <li>Recalcula {@code email_hmac} con la clave de cifrado real (reemplaza el SHA-256 sin clave
 *       que genera el script SQL de la migración V6 como bootstrap).
 *   <li>Deja que {@link StringEncryptedConverter} cifre el campo {@code email} en la próxima
 *       escritura. El conversor detecta valores sin el prefijo {@code enc:v1:} y los trata como
 *       texto plano.
 * </ul>
 *
 * <p>Se ejecuta una sola vez: si {@code email_hmac} ya coincide con {@code hmac(email)} el
 * registro no se toca.
 */
@Component
@Order(10)
public class EncryptionDataMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EncryptionDataMigrationRunner.class);

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public EncryptionDataMigrationRunner(
            UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var users = userRepository.findAll();
        int migrated = 0;

        for (var user : users) {
            String correctHmac = encryptionService.hmac(user.getEmail());
            if (!correctHmac.equals(user.getEmailHmac())) {
                user.setEmailHmac(correctHmac);
                userRepository.save(user);
                migrated++;
            }
        }

        if (migrated > 0) {
            log.info(
                    "Migración de cifrado completada: {} usuario(s) actualizados con email_hmac"
                            + " correcto.",
                    migrated);
        } else {
            log.debug("Migración de cifrado: todos los email_hmac ya están actualizados.");
        }
    }
}
