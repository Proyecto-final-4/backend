package com.backend.backend.domain.transaction;

import com.backend.backend.domain.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

    private static final int PERCENTAGE_SCALE = 4;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public SummaryService(
            TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String email, LocalDate from, LocalDate to) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo =
                to != null ? to : LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1);

        return buildSummaryForPeriod(user.getId(), effectiveFrom, effectiveTo);
    }

    @Transactional(readOnly = true)
    public TrendsResponse getTrends(
            String email,
            LocalDate currentFrom,
            LocalDate currentTo,
            LocalDate previousFrom,
            LocalDate previousTo) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        UUID userId = user.getId();
        SummaryResponse current = buildSummaryForPeriod(userId, currentFrom, currentTo);
        SummaryResponse previous = buildSummaryForPeriod(userId, previousFrom, previousTo);
        TrendsDiff diff = buildTrendsDiff(current, previous);

        return new TrendsResponse(current, previous, diff);
    }

    private SummaryResponse buildSummaryForPeriod(UUID userId, LocalDate from, LocalDate to) {
        Specification<Transaction> spec =
                Specification.where(TransactionSpecifications.hasUserId(userId))
                        .and(TransactionSpecifications.transactionDateBetween(from, to));

        List<Transaction> transactions = transactionRepository.findAll(spec);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        List<CategorySummary> byCategory = groupByCategory(transactions);

        return new SummaryResponse(totalIncome, totalExpense, balance, byCategory);
    }

    private TrendsDiff buildTrendsDiff(SummaryResponse current, SummaryResponse previous) {
        MetricChange incomeChange = metricChange(current.totalIncome(), previous.totalIncome());
        MetricChange expenseChange = metricChange(current.totalExpense(), previous.totalExpense());
        MetricChange balanceChange = metricChange(current.balance(), previous.balance());
        List<CategoryTrendDiff> byCategory = buildCategoryTrendDiffs(current, previous);

        return new TrendsDiff(incomeChange, expenseChange, balanceChange, byCategory);
    }

    private List<CategoryTrendDiff> buildCategoryTrendDiffs(
            SummaryResponse current, SummaryResponse previous) {
        Map<UUID, CategorySummary> currentById = indexByCategoryId(current.byCategory());
        Map<UUID, CategorySummary> previousById = indexByCategoryId(previous.byCategory());

        Set<UUID> categoryIds = new HashSet<>();
        categoryIds.addAll(currentById.keySet());
        categoryIds.addAll(previousById.keySet());

        return categoryIds.stream()
                .map(
                        id -> {
                            CategorySummary currentCat = currentById.get(id);
                            CategorySummary previousCat = previousById.get(id);
                            String name =
                                    currentCat != null
                                            ? currentCat.categoryName()
                                            : previousCat.categoryName();
                            BigDecimal currentTotal =
                                    currentCat != null ? currentCat.total() : BigDecimal.ZERO;
                            BigDecimal previousTotal =
                                    previousCat != null ? previousCat.total() : BigDecimal.ZERO;
                            BigDecimal change = currentTotal.subtract(previousTotal);
                            BigDecimal changePercentage =
                                    percentageChange(currentTotal, previousTotal);

                            return new CategoryTrendDiff(
                                    name, currentTotal, previousTotal, change, changePercentage);
                        })
                .sorted((a, b) -> a.categoryName().compareToIgnoreCase(b.categoryName()))
                .toList();
    }

    private Map<UUID, CategorySummary> indexByCategoryId(List<CategorySummary> summaries) {
        Map<UUID, CategorySummary> indexed = new HashMap<>();
        for (CategorySummary summary : summaries) {
            indexed.put(summary.categoryId(), summary);
        }
        return indexed;
    }

    private MetricChange metricChange(BigDecimal current, BigDecimal previous) {
        BigDecimal absolute = current.subtract(previous);
        BigDecimal percentage = percentageChange(current, previous);
        return new MetricChange(absolute, percentage);
    }

    private BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
            }
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
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
                                        t ->
                                                new CategoryKey(
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
