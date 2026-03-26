package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String type,
        LocalDate transactionDate,
        String description,
        String notes,
        Instant createdAt) {}
