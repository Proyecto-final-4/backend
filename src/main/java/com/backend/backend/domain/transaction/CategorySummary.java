package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySummary(UUID categoryId, String categoryName, BigDecimal total) {}
