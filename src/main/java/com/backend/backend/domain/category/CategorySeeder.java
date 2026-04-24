package com.backend.backend.domain.category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfAbsent("Alimentación", CategoryType.EXPENSE);
        seedIfAbsent("Transporte", CategoryType.EXPENSE);
        seedIfAbsent("Tecnología", CategoryType.EXPENSE);
        seedIfAbsent("Entretenimiento", CategoryType.EXPENSE);
        seedIfAbsent("Salud", CategoryType.EXPENSE);
        seedIfAbsent("Hogar", CategoryType.EXPENSE);
        seedIfAbsent("Educación", CategoryType.EXPENSE);
        seedIfAbsent("Ropa", CategoryType.EXPENSE);
        seedIfAbsent("Salario", CategoryType.INCOME);
        seedIfAbsent("Freelance", CategoryType.INCOME);
        seedIfAbsent("Inversiones", CategoryType.INCOME);
        seedIfAbsent("Otros", CategoryType.BOTH);
    }

    private void seedIfAbsent(String name, CategoryType type) {
        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            return;
        }
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setSystem(true);
        categoryRepository.save(category);
        log.info("Seeded system category: {}", name);
    }
}
