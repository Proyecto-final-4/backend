package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        UUID categoryId,
        BigDecimal amount,
        String type,
        LocalDate transactionDate,
        String description,
        String notes) {}
