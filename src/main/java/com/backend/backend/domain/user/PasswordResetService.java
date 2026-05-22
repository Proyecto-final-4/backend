package com.backend.backend.domain.user;

import com.backend.backend.shared.crypto.EncryptionService;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private static final String RESEND_URL = "https://api.resend.com/emails";
    private static final String INVALID_TOKEN = "Invalid or expired token";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final String resendApiKey;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EncryptionService encryptionService,
            @Value("${resend.api-key}") String resendApiKey) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.resendApiKey = resendApiKey;
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

        sendResetEmail(email, token);
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

    private void sendResetEmail(String to, String token) {
        String body =
                "{"
                        + "\"from\":\"onboarding@resend.dev\","
                        + "\"to\":[\""
                        + to
                        + "\"],"
                        + "\"subject\":\"Restablece tu contraseña — FinanzIA\","
                        + "\"html\":\"<p>Hola,</p>"
                        + "<p>Recibimos una solicitud para restablecer tu contraseña.</p>"
                        + "<p><a href='https://frontend-sooty-five-52.vercel.app/reset-password?token="
                        + token
                        + "'>Haz clic aquí para restablecer tu contraseña</a></p>"
                        + "<p>O copia este enlace en tu navegador:</p>"
                        + "<p>https://frontend-sooty-five-52.vercel.app/reset-password?token="
                        + token
                        + "</p>"
                        + "<p>Este enlace expira en 1 hora.</p>\""
                        + "}";

        HttpRequest httpRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(RESEND_URL))
                        .header("Authorization", "Bearer " + resendApiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

        try {
            HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
