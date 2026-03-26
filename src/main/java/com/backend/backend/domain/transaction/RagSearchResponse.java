package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RagSearchResponse(
        UUID id,
        String description,
        String notes,
        BigDecimal amount,
        String type,
        LocalDate transactionDate,
        String categoryName) {}
