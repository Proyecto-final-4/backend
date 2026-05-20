package com.backend.backend.shared;

import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.shared.crypto.EncryptionService;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public CurrentUserService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    /** Resolves the user by email, throwing if not found. */
    public User resolve(String email) {
        String emailHmac = encryptionService.hmac(email);
        return userRepository
                .findByEmailHmac(emailHmac)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
