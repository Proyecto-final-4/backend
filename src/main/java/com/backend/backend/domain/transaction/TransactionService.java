package com.backend.backend.domain.transaction;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public Page<TransactionResponse> getAll(
            String email,
            String type,
            UUID categoryId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        User user = findUserByEmail(email);

        TransactionType parsedType = type != null ? parseType(type) : null;

        Specification<Transaction> spec =
                Specification.where(TransactionSpecifications.hasUserId(user.getId()))
                        .and(
                                parsedType != null
                                        ? TransactionSpecifications.hasType(parsedType)
                                        : null)
                        .and(
                                categoryId != null
                                        ? TransactionSpecifications.hasCategoryId(categoryId)
                                        : null)
                        .and(
                                from != null && to != null
                                        ? TransactionSpecifications.transactionDateBetween(from, to)
                                        : null);

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TransactionResponse getById(String email, UUID id) {
        User user = findUserByEmail(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);
        return toResponse(transaction);
    }

    public TransactionResponse create(String email, TransactionRequest request) {
        User user = findUserByEmail(email);
        Category category = findCategoryForUser(request.categoryId(), user);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(request.amount());
        transaction.setType(parseType(request.type()));
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description());
        transaction.setNotes(request.notes());

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public TransactionResponse update(String email, UUID id, TransactionRequest request) {
        User user = findUserByEmail(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);

        if (request.categoryId() != null) {
            transaction.setCategory(findCategoryForUser(request.categoryId(), user));
        }
        if (request.amount() != null) {
            transaction.setAmount(request.amount());
        }
        if (request.type() != null) {
            transaction.setType(parseType(request.type()));
        }
        if (request.transactionDate() != null) {
            transaction.setTransactionDate(request.transactionDate());
        }
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }
        if (request.notes() != null) {
            transaction.setNotes(request.notes());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public void delete(String email, UUID id) {
        User user = findUserByEmail(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);
        transactionRepository.delete(transaction);
    }

    private User findUserByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Transaction findTransactionById(UUID id) {
        return transactionRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    private Category findCategoryForUser(UUID categoryId, User user) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.isSystem()
                && (category.getUser() == null
                        || !category.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("Category does not belong to user");
        }
        return category;
    }

    private void validateOwnership(Transaction transaction, User user) {
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Transaction does not belong to user");
        }
    }

    private TransactionType parseType(String type) {
        try {
            return TransactionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid transaction type: " + type);
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getNotes(),
                transaction.getCreatedAt());
    }
}
