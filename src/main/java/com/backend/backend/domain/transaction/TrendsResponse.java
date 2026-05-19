package com.backend.backend.domain.transaction;

public record TrendsResponse(SummaryResponse current, SummaryResponse previous, TrendsDiff diff) {}
