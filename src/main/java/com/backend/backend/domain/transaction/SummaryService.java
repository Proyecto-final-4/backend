package com.backend.backend.domain.transaction;

import com.backend.backend.domain.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public SummaryService(
            TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public SummaryResponse getSummary(String email, LocalDate from, LocalDate to) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1);

        Specification<Transaction> spec =
                Specification.where(TransactionSpecifications.hasUserId(user.getId()))
                        .and(TransactionSpecifications.transactionDateBetween(effectiveFrom, effectiveTo));

        List<Transaction> transactions = transactionRepository.findAll(spec);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        List<CategorySummary> byCategory = groupByCategory(transactions);

        return new SummaryResponse(totalIncome, totalExpense, balance, byCategory);
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategorySummary> groupByCategory(List<Transaction> transactions) {
        Map<CategoryKey, BigDecimal> totals =
                transactions.stream()
                        .collect(
                                Collectors.groupingBy(
                                        t -> new CategoryKey(
                                                t.getCategory().getId(),
                                                t.getCategory().getName()),
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                Transaction::getAmount,
                                                BigDecimal::add)));

        return totals.entrySet().stream()
                .map(e -> new CategorySummary(e.getKey().id(), e.getKey().name(), e.getValue()))
                .toList();
    }

    private record CategoryKey(UUID id, String name) {}
}
