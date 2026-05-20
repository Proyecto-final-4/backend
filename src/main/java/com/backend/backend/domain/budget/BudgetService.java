package com.backend.backend.domain.budget;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.category.CategoryType;
import com.backend.backend.domain.transaction.TransactionRepository;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.shared.crypto.EncryptionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EncryptionService encryptionService;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            EncryptionService encryptionService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll(String email) {
        User user = findUserByEmail(email);
        return budgetRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse create(String email, BudgetRequest request) {
        User user = findUserByEmail(email);
        validateCreateRequest(request);

        Category category = resolveExpenseCategory(request.categoryId(), user);

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setAmountLimit(request.amountLimit());
        budget.setPeriod(parsePeriod(request.period()));
        budget.setStartDate(request.startDate());
        budget.setEndDate(request.endDate());
        budget.setActive(request.isActive() == null || request.isActive());

        budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse update(String email, UUID id, BudgetRequest request) {
        User user = findUserByEmail(email);
        Budget budget = findBudgetForUser(id, user);

        if (request.categoryId() != null) {
            budget.setCategory(resolveExpenseCategory(request.categoryId(), user));
        }
        if (request.amountLimit() != null) {
            validateAmountLimit(request.amountLimit());
            budget.setAmountLimit(request.amountLimit());
        }
        if (request.period() != null) {
            budget.setPeriod(parsePeriod(request.period()));
        }
        if (request.startDate() != null) {
            budget.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            budget.setEndDate(request.endDate());
        }
        if (request.isActive() != null) {
            budget.setActive(request.isActive());
        }

        validateDateRange(budget.getStartDate(), budget.getEndDate());

        budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public void delete(String email, UUID id) {
        User user = findUserByEmail(email);
        Budget budget = findBudgetForUser(id, user);
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public BudgetStatusResponse getStatus(String email, UUID id) {
        User user = findUserByEmail(email);
        Budget budget = findBudgetForUser(id, user);
        return buildStatus(budget, user.getId());
    }

    private BudgetStatusResponse buildStatus(Budget budget, UUID userId) {
        BigDecimal limit = budget.getAmountLimit();
        LocalDate today = LocalDate.now();

        return BudgetPeriodResolver.resolve(budget, today)
                .map(
                        window -> {
                            BigDecimal spent =
                                    transactionRepository
                                            .sumExpenseAmountByUserCategoryAndDateRange(
                                                    userId,
                                                    budget.getCategory().getId(),
                                                    window.start(),
                                                    window.end());
                            BigDecimal remaining = limit.subtract(spent);
                            BigDecimal percentage = calculatePercentage(spent, limit);
                            return new BudgetStatusResponse(
                                    spent, remaining, percentage, window.start(), window.end());
                        })
                .orElseGet(
                        () ->
                                new BudgetStatusResponse(
                                        BigDecimal.ZERO, limit, BigDecimal.ZERO, null, null));
    }

    private BigDecimal calculatePercentage(BigDecimal spent, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return spent.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
    }

    private void validateCreateRequest(BudgetRequest request) {
        if (request.categoryId() == null) {
            throw new RuntimeException("categoryId is required");
        }
        if (request.amountLimit() == null) {
            throw new RuntimeException("amountLimit is required");
        }
        if (request.period() == null || request.period().isBlank()) {
            throw new RuntimeException("period is required");
        }
        if (request.startDate() == null) {
            throw new RuntimeException("startDate is required");
        }
        validateAmountLimit(request.amountLimit());
        validateDateRange(request.startDate(), request.endDate());
    }

    private void validateAmountLimit(BigDecimal amountLimit) {
        if (amountLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amountLimit must be greater than zero");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("endDate must be on or after startDate");
        }
    }

    private Category resolveExpenseCategory(UUID categoryId, User user) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.isSystem()
                && (category.getUser() == null
                        || !category.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("Category does not belong to user");
        }

        CategoryType type = category.getType();
        if (type != CategoryType.EXPENSE && type != CategoryType.BOTH) {
            throw new RuntimeException(
                    "Budgets can only be created for expense categories (EXPENSE or BOTH)");
        }

        return category;
    }

    private BudgetPeriod parsePeriod(String period) {
        try {
            return BudgetPeriod.valueOf(period);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid budget period: " + period);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository
                .findByEmailHmac(encryptionService.hmac(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Budget findBudgetForUser(UUID id, User user) {
        return budgetRepository
                .findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmountLimit(),
                budget.getPeriod().name(),
                budget.getStartDate(),
                budget.getEndDate(),
                budget.isActive(),
                budget.getCreatedAt());
    }
}
