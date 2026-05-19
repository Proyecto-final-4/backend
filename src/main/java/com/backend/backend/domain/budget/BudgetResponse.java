package com.backend.backend.domain.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amountLimit,
        String period,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive,
        Instant createdAt) {}
