package com.backend.backend.domain.transaction;

import java.math.BigDecimal;

public record CategoryTrendDiff(
        String categoryName,
        BigDecimal currentTotal,
        BigDecimal previousTotal,
        BigDecimal change,
        BigDecimal changePercentage) {}
