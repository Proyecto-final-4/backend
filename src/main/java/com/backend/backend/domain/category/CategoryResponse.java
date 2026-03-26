package com.backend.backend.domain.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String type,
        String color,
        String icon,
        boolean isSystem,
        UUID parentId) {}
