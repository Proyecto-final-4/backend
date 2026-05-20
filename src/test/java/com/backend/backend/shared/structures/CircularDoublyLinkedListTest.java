package com.backend.backend.shared.structures;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.backend.domain.budget.BudgetPeriod;
import org.junit.jupiter.api.Test;

class CircularDoublyLinkedListTest {

    @Test
    void startsEmpty() {
        CircularDoublyLinkedList<String> list = new CircularDoublyLinkedList<>();

        assertThat(list.isEmpty()).isTrue();
        assertThat(list.size()).isZero();
        assertThat(list.toList()).isEmpty();
    }

    @Test
    void addContainsSizeAndForwardOrder() {
        CircularDoublyLinkedList<BudgetPeriod> list = new CircularDoublyLinkedList<>();

        list.addLast(BudgetPeriod.DAILY);
        list.addLast(BudgetPeriod.WEEKLY);
        list.addLast(BudgetPeriod.MONTHLY);

        assertThat(list.isEmpty()).isFalse();
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.contains(BudgetPeriod.DAILY)).isTrue();
        assertThat(list.contains(BudgetPeriod.WEEKLY)).isTrue();
        assertThat(list.contains(BudgetPeriod.MONTHLY)).isTrue();
        assertThat(list.contains(null)).isFalse();
        assertThat(list.toList())
                .containsExactly(BudgetPeriod.DAILY, BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY);
        assertThat(list.traverseForward())
                .containsExactly(BudgetPeriod.DAILY, BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY);
    }
}
