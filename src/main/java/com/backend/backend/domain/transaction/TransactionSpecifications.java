package com.backend.backend.domain.transaction;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> transactionDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("transactionDate"), from, to);
    }
}
