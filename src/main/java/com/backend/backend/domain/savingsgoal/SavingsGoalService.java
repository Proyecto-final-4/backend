package com.backend.backend.domain.savingsgoal;

import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.shared.crypto.EncryptionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            UserRepository userRepository,
            EncryptionService encryptionService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getAll(String email) {
        var user = findUserByEmail(email);
        return savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse create(String email, GoalRequest request) {
        var user = findUserByEmail(email);
        validateCreateRequest(request);

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(request.name().trim());
        goal.setDescription(request.description());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setTargetDate(request.targetDate());
        goal.setCompleted(false);

        savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse update(String email, UUID id, GoalUpdateRequest request) {
        var user = findUserByEmail(email);
        SavingsGoal goal = findGoalForUser(id, user.getId());

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new RuntimeException("Goal name cannot be blank.");
            }
            goal.setName(request.name().trim());
        }

        if (request.description() != null) {
            goal.setDescription(request.description());
        }

        if (request.targetAmount() != null) {
            validateTargetAmount(request.targetAmount());
            goal.setTargetAmount(request.targetAmount());
        }

        if (request.currentAmount() != null) {
            validateCurrentAmount(request.currentAmount());
            goal.setCurrentAmount(request.currentAmount());
        }

        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }

        if (request.isCompleted() != null) {
            goal.setCompleted(request.isCompleted());
        }

        savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public void delete(String email, UUID id) {
        var user = findUserByEmail(email);
        SavingsGoal goal = findGoalForUser(id, user.getId());
        savingsGoalRepository.delete(goal);
    }

    private User findUserByEmail(String email) {
        return userRepository
                .findByEmailHmac(encryptionService.hmac(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private SavingsGoal findGoalForUser(UUID id, UUID userId) {
        return savingsGoalRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Savings goal not found"));
    }

    private void validateCreateRequest(GoalRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new RuntimeException("Goal name is required.");
        }
        if (request.targetAmount() == null) {
            throw new RuntimeException("Target amount is required.");
        }
        validateTargetAmount(request.targetAmount());
    }

    private void validateTargetAmount(BigDecimal targetAmount) {
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Target amount must be greater than zero.");
        }
    }

    private void validateCurrentAmount(BigDecimal currentAmount) {
        if (currentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Current amount cannot be negative.");
        }
    }

    private GoalResponse toResponse(SavingsGoal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getDescription(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.isCompleted(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
