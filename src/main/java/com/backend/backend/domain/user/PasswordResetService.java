package com.backend.backend.domain.user;

import com.backend.backend.shared.crypto.EncryptionService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private static final String SENDER = "estructurasypatrones@gmail.com";
    private static final String INVALID_TOKEN = "Invalid or expired token";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EncryptionService encryptionService,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.mailSenderProvider = mailSenderProvider;
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email() == null ? "" : request.email().trim();
        String emailHmac = encryptionService.hmac(email);
        User user = userRepository.findByEmailHmac(emailHmac).orElse(null);
        if (user == null) {
            return;
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(SENDER);
            msg.setTo(email);
            msg.setSubject("Password Reset Request");
            msg.setText(
                    "Your password reset token is: " + token + "\n\nThis token expires in 1 hour.");
            mailSender.send(msg);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token()).orElse(null);
        if (resetToken == null
                || resetToken.isUsed()
                || Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new RuntimeException(INVALID_TOKEN);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
