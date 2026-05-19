package com.backend.backend.domain.budget;

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
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> getAll() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return budgetService.getAll(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@RequestBody BudgetRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return budgetService.create(email, request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable UUID id, @RequestBody BudgetRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return budgetService.update(email, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        budgetService.delete(email, id);
    }

    @GetMapping("/{id}/status")
    public BudgetStatusResponse getStatus(@PathVariable UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return budgetService.getStatus(email, id);
    }
}
