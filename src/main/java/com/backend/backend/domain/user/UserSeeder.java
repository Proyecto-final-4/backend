package com.backend.backend.domain.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_USER_EMAIL:}")
    private String email;

    @Value("${SEED_USER_PASSWORD:}")
    private String password;

    @Value("${SEED_USER_NAME:}")
    private String name;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            log.debug("SEED_USER_EMAIL not set — skipping user seeding");
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn(
                    "SEED_USER_EMAIL is set but SEED_USER_PASSWORD is missing — skipping user seeding");
            return;
        }

        if (name == null || name.isBlank()) {
            name = email;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Seed user '{}' already exists — nothing to do", email);
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);

        log.info("Seed user '{}' created successfully", email);
    }
}
