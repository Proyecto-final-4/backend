package com.backend.backend.shared.crypto;

import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.shared.structures.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Queue<User> queue = new Queue<>();
        int migrated = 0;
        int pageNumber = 0;
        Page<User> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, 50);
            page = userRepository.findAll(pageable);
            for (User user : page.getContent()) {
                queue.enqueue(user);
            }
            while (!queue.isEmpty()) {
                User user = queue.dequeue();
                String correctHmac = encryptionService.hmac(user.getEmail());
                if (!correctHmac.equals(user.getEmailHmac())) {
                    user.setEmailHmac(correctHmac);
                    userRepository.save(user);
                    migrated++;
                }
            }
            pageNumber++;
        } while (page.hasNext());

        if (migrated > 0) {
            log.info(
                    "Encryption migration completed: {} user(s) updated with correct email_hmac.",
                    migrated);
        } else {
            log.debug("Encryption migration: all email_hmac values are already up to date.");
        }
    }
}
