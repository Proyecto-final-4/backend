package com.backend.backend.domain.savingsgoal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        String name, String description, BigDecimal targetAmount, LocalDate targetDate) {}
