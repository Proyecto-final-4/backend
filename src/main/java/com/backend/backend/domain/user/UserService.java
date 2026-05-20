package com.backend.backend.domain.user;

import com.backend.backend.shared.crypto.EncryptionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
    }

    public UserResponse getCurrentUser(String email) {
        User user =
                userRepository
                        .findByEmailHmac(encryptionService.hmac(email))
                        .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public UserResponse updateUser(String email, UpdateUserRequest request) {
        User user =
                userRepository
                        .findByEmailHmac(encryptionService.hmac(email))
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.currentPassword() != null && request.newPassword() != null) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new RuntimeException("Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
