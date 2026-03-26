package com.backend.backend.domain.category;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return categoryService.getAll(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestBody CategoryRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return categoryService.create(email, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @RequestBody CategoryRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return categoryService.update(email, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        categoryService.delete(email, id);
    }
}
