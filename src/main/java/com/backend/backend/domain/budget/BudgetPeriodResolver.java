package com.backend.backend.domain.budget;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

final class BudgetPeriodResolver {

    private BudgetPeriodResolver() {}

    static Optional<PeriodWindow> resolve(Budget budget, LocalDate today) {
        if (today.isBefore(budget.getStartDate())) {
            return Optional.empty();
        }

        LocalDate effectiveDate = today;
        if (budget.getEndDate() != null && today.isAfter(budget.getEndDate())) {
            effectiveDate = budget.getEndDate();
        }

        LocalDate periodStart;
        LocalDate periodEnd;

        switch (budget.getPeriod()) {
            case DAILY -> {
                periodStart = effectiveDate;
                periodEnd = effectiveDate;
            }
            case WEEKLY -> {
                periodStart = effectiveDate.with(DayOfWeek.MONDAY);
                periodEnd = periodStart.plusDays(6);
            }
            case MONTHLY -> {
                periodStart = effectiveDate.withDayOfMonth(1);
                periodEnd = effectiveDate.with(TemporalAdjusters.lastDayOfMonth());
            }
            default -> throw new IllegalStateException("Unexpected period: " + budget.getPeriod());
        }

        if (periodStart.isBefore(budget.getStartDate())) {
            periodStart = budget.getStartDate();
        }
        if (budget.getEndDate() != null && periodEnd.isAfter(budget.getEndDate())) {
            periodEnd = budget.getEndDate();
        }

        if (periodStart.isAfter(periodEnd)) {
            return Optional.empty();
        }

        return Optional.of(new PeriodWindow(periodStart, periodEnd));
    }

    record PeriodWindow(LocalDate start, LocalDate end) {}
}
