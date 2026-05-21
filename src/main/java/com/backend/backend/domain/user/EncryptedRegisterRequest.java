package com.backend.backend.domain.user;

import jakarta.validation.constraints.NotBlank;

public record EncryptedRegisterRequest(
        @NotBlank String encryptedName,
        @NotBlank String encryptedEmail,
        @NotBlank String encryptedPassword) {}
