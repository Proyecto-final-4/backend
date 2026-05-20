package com.backend.backend.domain.user;

public record EncryptedRegisterRequest(
        String encryptedName, String encryptedEmail, String encryptedPassword) {}
