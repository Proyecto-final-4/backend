package com.backend.backend.domain.transaction;

import java.math.BigDecimal;

public record MetricChange(BigDecimal absolute, BigDecimal percentage) {}
