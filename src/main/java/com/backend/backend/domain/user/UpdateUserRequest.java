package com.backend.backend.domain.user;

public record UpdateUserRequest(String name, String currentPassword, String newPassword) {}
