package com.backend.backend.domain.transaction;

import com.backend.backend.shared.CurrentUserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

    private static final int PERCENTAGE_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public SummaryService(
            TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String email, LocalDate from, LocalDate to) {
        var user = currentUserService.resolve(email);

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
        var user = currentUserService.resolve(email);

        UUID userId = user.getId();
        SummaryResponse current = buildSummaryForPeriod(userId, currentFrom, currentTo);
        SummaryResponse previous = buildSummaryForPeriod(userId, previousFrom, previousTo);
        TrendsDiff diff = buildTrendsDiff(current, previous);

        return new TrendsResponse(current, previous, diff);
    }

    private SummaryResponse buildSummaryForPeriod(UUID userId, LocalDate from, LocalDate to) {
        return buildSummaryOptimized(userId, from, to);
    }

    /**
     * Optimized version: delegates aggregations to the database instead of loading all transactions
     * into memory and grouping with JVM streams.
     */
    private SummaryResponse buildSummaryOptimized(UUID userId, LocalDate from, LocalDate to) {
        // Totals by type (INCOME / EXPENSE) from the database
        List<Object[]> typeTotals = transactionRepository.sumAmountByTypeForUser(userId, from, to);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Object[] row : typeTotals) {
            TransactionType type = (TransactionType) row[0];
            BigDecimal sum = ((BigDecimal) row[1]).setScale(0, RoundingMode.HALF_UP);
            if (type == TransactionType.INCOME) totalIncome = sum;
            else if (type == TransactionType.EXPENSE) totalExpense = sum;
        }

        // Category and type breakdown from the database
        List<Object[]> categoryRows =
                transactionRepository.sumAmountByCategoryAndTypeForUser(userId, from, to);

        List<CategorySummary> rawIncome = new ArrayList<>();
        List<CategorySummary> rawExpense = new ArrayList<>();
        for (Object[] row : categoryRows) {
            UUID categoryId = (UUID) row[0];
            String categoryName = (String) row[1];
            TransactionType type = (TransactionType) row[2];
            BigDecimal sum = ((BigDecimal) row[3]).setScale(0, RoundingMode.HALF_UP);
            CategorySummary entry =
                    new CategorySummary(categoryId, categoryName, sum, BigDecimal.ZERO);
            if (type == TransactionType.INCOME) rawIncome.add(entry);
            else if (type == TransactionType.EXPENSE) rawExpense.add(entry);
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);
        List<CategorySummary> incomeByCategory = withPercentages(rawIncome, totalIncome);
        List<CategorySummary> expenseByCategory = withPercentages(rawExpense, totalExpense);
        BigDecimal savingsRate = calculateSavingsRate(balance, totalIncome);

        return new SummaryResponse(
                totalIncome,
                totalExpense,
                balance,
                savingsRate,
                incomeByCategory,
                expenseByCategory);
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
        Map<UUID, CategorySummary> currentById = indexByCategoryId(allCategories(current));
        Map<UUID, CategorySummary> previousById = indexByCategoryId(allCategories(previous));

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

    private List<CategorySummary> allCategories(SummaryResponse summary) {
        List<CategorySummary> merged = new ArrayList<>();
        merged.addAll(summary.incomeByCategory());
        merged.addAll(summary.expenseByCategory());
        return merged;
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
                .multiply(ONE_HUNDRED)
                .divide(previous, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSavingsRate(BigDecimal balance, BigDecimal totalIncome) {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return balance.multiply(ONE_HUNDRED)
                .divide(totalIncome, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private List<CategorySummary> withPercentages(
            List<CategorySummary> categories, BigDecimal typeTotal) {
        if (typeTotal.compareTo(BigDecimal.ZERO) == 0) {
            return categories.stream()
                    .map(
                            category ->
                                    new CategorySummary(
                                            category.categoryId(),
                                            category.categoryName(),
                                            category.total(),
                                            BigDecimal.ZERO.setScale(
                                                    PERCENTAGE_SCALE, RoundingMode.HALF_UP)))
                    .toList();
        }
        return categories.stream()
                .map(
                        category ->
                                new CategorySummary(
                                        category.categoryId(),
                                        category.categoryName(),
                                        category.total(),
                                        category.total()
                                                .multiply(ONE_HUNDRED)
                                                .divide(
                                                        typeTotal,
                                                        PERCENTAGE_SCALE,
                                                        RoundingMode.HALF_UP)))
                .toList();
    }
}
