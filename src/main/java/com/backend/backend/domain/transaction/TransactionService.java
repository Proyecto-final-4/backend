package com.backend.backend.domain.transaction;

import com.backend.backend.domain.category.Category;
import com.backend.backend.domain.category.CategoryRepository;
import com.backend.backend.domain.category.CategoryType;
import com.backend.backend.domain.user.User;
import com.backend.backend.shared.CurrentUserService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final EmbeddingService embeddingService;
    private final CurrentUserService currentUserService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            EmbeddingService embeddingService,
            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.embeddingService = embeddingService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(
            String email,
            String type,
            UUID categoryId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        User user = currentUserService.resolve(email);

        TransactionType parsedType = type != null ? parseType(type) : null;

        Specification<Transaction> spec =
                Specification.where(TransactionSpecifications.hasUserId(user.getId()));

        if (parsedType != null) {
            spec = spec.and(TransactionSpecifications.hasType(parsedType));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecifications.hasCategoryId(categoryId));
        }
        if (from != null && to != null) {
            spec = spec.and(TransactionSpecifications.transactionDateBetween(from, to));
        } else if (from != null) {
            spec = spec.and(TransactionSpecifications.transactionDateFrom(from));
        } else if (to != null) {
            spec = spec.and(TransactionSpecifications.transactionDateTo(to));
        }

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TransactionResponse getById(String email, UUID id) {
        User user = currentUserService.resolve(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse create(String email, TransactionRequest request) {
        User user = currentUserService.resolve(email);
        Category category = findCategoryForUser(request.categoryId(), user);
        TransactionType transactionType = parseType(request.type());
        validateCategoryMatchesTransactionType(category, transactionType);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(request.amount());
        transaction.setType(transactionType);
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description());
        transaction.setNotes(request.notes());

        transactionRepository.save(transaction);

        float[] embedding = embeddingService.generateEmbedding(transaction.getDescription());
        transactionRepository.updateEmbedding(transaction.getId(), buildVectorString(embedding));

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(String email, UUID id, TransactionRequest request) {
        User user = currentUserService.resolve(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);

        if (request.categoryId() != null) {
            Category newCategory = findCategoryForUser(request.categoryId(), user);
            TransactionType newType =
                    request.type() != null ? parseType(request.type()) : transaction.getType();
            validateCategoryMatchesTransactionType(newCategory, newType);
            transaction.setCategory(newCategory);
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

        if (request.description() != null) {
            float[] embedding = embeddingService.generateEmbedding(request.description());
            transactionRepository.updateEmbedding(
                    transaction.getId(), buildVectorString(embedding));
        }

        return toResponse(transaction);
    }

    public void delete(String email, UUID id) {
        User user = currentUserService.resolve(email);
        Transaction transaction = findTransactionById(id);
        validateOwnership(transaction, user);
        transactionRepository.delete(transaction);
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

    private void validateCategoryMatchesTransactionType(
            Category category, TransactionType transactionType) {
        CategoryType catType = category.getType();
        if (catType == CategoryType.BOTH) return;
        if (transactionType == TransactionType.INCOME && catType != CategoryType.INCOME) {
            throw new RuntimeException(
                    "Category '"
                            + category.getName()
                            + "' is for expenses and cannot be used for an income transaction.");
        }
        if (transactionType == TransactionType.EXPENSE && catType != CategoryType.EXPENSE) {
            throw new RuntimeException(
                    "Category '"
                            + category.getName()
                            + "' is for income and cannot be used for an expense transaction.");
        }
    }

    private TransactionType parseType(String type) {
        try {
            return TransactionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid transaction type: " + type);
        }
    }

    private String buildVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
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
