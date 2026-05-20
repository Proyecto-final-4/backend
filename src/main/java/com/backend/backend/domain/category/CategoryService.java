package com.backend.backend.domain.category;

import com.backend.backend.domain.transaction.TransactionRepository;
import com.backend.backend.domain.user.User;
import com.backend.backend.domain.user.UserRepository;
import com.backend.backend.shared.crypto.EncryptionService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EncryptionService encryptionService;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            EncryptionService encryptionService) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.encryptionService = encryptionService;
    }

    public List<CategoryResponse> getAll(String email) {
        var user = findUserByEmail(email);
        return categoryRepository.findByUserIsNullOrUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse create(String email, CategoryRequest request) {
        var user = findUserByEmail(email);

        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(request.name())) {
            throw new RuntimeException(
                    "A system category named '"
                            + request.name()
                            + "' already exists. Use it directly instead of creating a duplicate.");
        }
        if (categoryRepository.existsByNameIgnoreCaseAndUserId(request.name(), user.getId())) {
            throw new RuntimeException(
                    "You already have a category named '" + request.name() + "'.");
        }

        CategoryType type;
        try {
            type = CategoryType.valueOf(request.type());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid category type: " + request.type());
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent =
                    categoryRepository
                            .findById(request.parentId())
                            .orElseThrow(() -> new RuntimeException("Parent category not found"));
        }

        Category category = new Category();
        category.setName(request.name());
        category.setType(type);
        category.setColor(request.color());
        category.setIcon(request.icon());
        category.setSystem(false);
        category.setUser(user);
        category.setParent(parent);

        categoryRepository.save(category);
        return toResponse(category);
    }

    public CategoryResponse update(String email, UUID id, CategoryRequest request) {
        var user = findUserByEmail(email);

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isSystem()) {
            throw new RuntimeException(
                    "System category '"
                            + category.getName()
                            + "' cannot be modified. Create a personal category instead.");
        }
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Category does not belong to user.");
        }

        if (request.name() != null) {
            category.setName(request.name());
        }

        if (request.type() != null) {
            try {
                category.setType(CategoryType.valueOf(request.type()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid category type: " + request.type());
            }
        }

        if (request.color() != null) {
            category.setColor(request.color());
        }

        if (request.icon() != null) {
            category.setIcon(request.icon());
        }

        if (request.parentId() != null) {
            Category parent =
                    categoryRepository
                            .findById(request.parentId())
                            .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }

        categoryRepository.save(category);
        return toResponse(category);
    }

    public void delete(String email, UUID id) {
        var user = findUserByEmail(email);

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isSystem()) {
            throw new RuntimeException(
                    "System category '" + category.getName() + "' cannot be deleted.");
        }
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Category does not belong to user.");
        }
        if (transactionRepository.existsByCategoryId(id)) {
            throw new RuntimeException(
                    "Cannot delete category '"
                            + category.getName()
                            + "' because it has associated transactions. Reassign them to another"
                            + " category first.");
        }

        categoryRepository.delete(category);
    }

    private User findUserByEmail(String email) {
        return userRepository
                .findByEmailHmac(encryptionService.hmac(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType().name(),
                category.getColor(),
                category.getIcon(),
                category.isSystem(),
                category.getParent() != null ? category.getParent().getId() : null);
    }
}
