package com.backend.backend.domain.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email HMAC. Use {@link
     * com.backend.backend.shared.crypto.EncryptionService#hmac(String)} to compute the value.
     */
    Optional<User> findByEmailHmac(String emailHmac);
}
