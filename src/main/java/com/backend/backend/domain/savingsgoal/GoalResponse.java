package com.backend.backend.domain.savingsgoal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        String description,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        boolean isCompleted,
        Instant createdAt,
        Instant updatedAt) {}
