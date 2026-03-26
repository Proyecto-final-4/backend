package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategorySummary> byCategory) {

    public SummaryResponse {
        byCategory = byCategory == null ? List.of() : List.copyOf(byCategory);
    }
}
