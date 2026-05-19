package com.backend.backend.domain.transaction;

import java.util.List;

public record TrendsDiff(
        MetricChange incomeChange,
        MetricChange expenseChange,
        MetricChange balanceChange,
        List<CategoryTrendDiff> byCategory) {

    public TrendsDiff {
        byCategory = byCategory == null ? List.of() : List.copyOf(byCategory);
    }
}
