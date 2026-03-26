package com.backend.backend.domain.user;

import java.util.UUID;

public record AuthResponse(String token, UUID id, String name, String email) {}
