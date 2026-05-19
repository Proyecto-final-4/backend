package com.backend.backend.domain.savingsgoal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalUpdateRequest(
        String name,
        String description,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        Boolean isCompleted) {}
