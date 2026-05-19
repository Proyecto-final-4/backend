package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        BigDecimal savingsRate,
        List<CategorySummary> incomeByCategory,
        List<CategorySummary> expenseByCategory) {

    public SummaryResponse {
        incomeByCategory = incomeByCategory == null ? List.of() : List.copyOf(incomeByCategory);
        expenseByCategory = expenseByCategory == null ? List.of() : List.copyOf(expenseByCategory);
    }
}
