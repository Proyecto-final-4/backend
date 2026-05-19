package com.backend.backend.domain.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRequest(
        UUID categoryId,
        BigDecimal amountLimit,
        String period,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isActive) {}
