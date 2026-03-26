package com.backend.backend.domain.category;

import com.backend.backend.domain.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<CategoryResponse> getAll(String email) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        return categoryRepository.findByUserIsNullOrUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse create(String email, CategoryRequest request) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

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
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isSystem()
                || category.getUser() == null
                || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Category does not belong to user");
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
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isSystem()
                || category.getUser() == null
                || !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Category does not belong to user");
        }

        categoryRepository.delete(category);
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
