package com.backend.backend.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BudgetPeriodRegistryTest {

    @Test
    void allEnumValuesAreSupportedInRingOrder() {
        assertThat(BudgetPeriodRegistry.isSupported(BudgetPeriod.DAILY)).isTrue();
        assertThat(BudgetPeriodRegistry.isSupported(BudgetPeriod.WEEKLY)).isTrue();
        assertThat(BudgetPeriodRegistry.isSupported(BudgetPeriod.MONTHLY)).isTrue();
        assertThat(BudgetPeriodRegistry.isSupported(null)).isFalse();

        assertThat(BudgetPeriodRegistry.allInRingOrder())
                .containsExactly(BudgetPeriod.DAILY, BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY);

        for (BudgetPeriod period : BudgetPeriod.values()) {
            assertThat(BudgetPeriodRegistry.isSupported(period)).isTrue();
        }
    }
}
