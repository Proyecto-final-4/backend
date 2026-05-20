package com.backend.backend.domain.budget;

import com.backend.backend.shared.structures.CircularDoublyLinkedList;
import java.util.List;

/** Registry of supported {@link BudgetPeriod} values stored in a circular doubly linked ring. */
public final class BudgetPeriodRegistry {

    private static final CircularDoublyLinkedList<BudgetPeriod> RING =
            new CircularDoublyLinkedList<>();

    static {
        RING.addLast(BudgetPeriod.DAILY);
        RING.addLast(BudgetPeriod.WEEKLY);
        RING.addLast(BudgetPeriod.MONTHLY);
    }

    private BudgetPeriodRegistry() {}

    public static boolean isSupported(BudgetPeriod period) {
        return period != null && RING.contains(period);
    }

    public static List<BudgetPeriod> allInRingOrder() {
        return RING.toList();
    }
}
