package com.backend.backend.domain.user;

public record EncryptedLoginRequest(String encryptedEmail, String encryptedPassword) {}
