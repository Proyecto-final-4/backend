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
 * Hot-migrates user records created before encryption was enabled:
 *
 * <ul>
 *   <li>Recalculates {@code email_hmac} with the real encryption key (replaces the keyless SHA-256
 *       produced by the V6 migration SQL script as bootstrap).
 *   <li>Lets {@link StringEncryptedConverter} encrypt the {@code email} field on the next write.
 *       The converter treats values without the {@code enc:v1:} prefix as plaintext.
 * </ul>
 *
 * <p>Runs once: if {@code email_hmac} already matches {@code hmac(email)} the record is skipped.
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
                    "Encryption migration completed: {} user(s) updated with correct email_hmac.",
                    migrated);
        } else {
            log.debug("Encryption migration: all email_hmac values are already up to date.");
        }
    }
}
