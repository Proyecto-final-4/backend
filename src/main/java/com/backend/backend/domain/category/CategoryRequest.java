package com.backend.backend.domain.category;

import java.util.UUID;

public record CategoryRequest(String name, String type, String color, String icon, UUID parentId) {}
