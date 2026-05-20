package com.backend.backend.domain.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca un usuario por el HMAC de su email. Usar {@link
     * com.backend.backend.shared.crypto.EncryptionService#hmac(String)} para calcular el valor.
     */
    Optional<User> findByEmailHmac(String emailHmac);
}
