package com.backend.backend.domain.user;

import com.backend.backend.shared.crypto.RsaKeyService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RsaKeyService rsaKeyService;

    public AuthController(AuthService authService, RsaKeyService rsaKeyService) {
        this.authService = authService;
        this.rsaKeyService = rsaKeyService;
    }

    /**
     * Returns the RSA-2048 public key (SPKI/Base64) so the client can encrypt credentials before
     * sending them. The key is ephemeral and regenerated on every startup.
     */
    @GetMapping("/public-key")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getPublicKey() {
        return Map.of("publicKey", rsaKeyService.getPublicKeyBase64());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody EncryptedRegisterRequest request) {
        String name = rsaKeyService.decrypt(request.encryptedName());
        String email = rsaKeyService.decrypt(request.encryptedEmail());
        String password = rsaKeyService.decrypt(request.encryptedPassword());
        return authService.register(new RegisterRequest(name, email, password));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@RequestBody EncryptedLoginRequest request) {
        String email = rsaKeyService.decrypt(request.encryptedEmail());
        String password = rsaKeyService.decrypt(request.encryptedPassword());
        return authService.login(new LoginRequest(email, password));
    }
}
