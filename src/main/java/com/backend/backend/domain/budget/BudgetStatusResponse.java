package com.backend.backend.domain.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetStatusResponse(
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentage,
        LocalDate periodStart,
        LocalDate periodEnd) {}
