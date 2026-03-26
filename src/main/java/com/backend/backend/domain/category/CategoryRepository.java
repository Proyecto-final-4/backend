package com.backend.backend.domain.category;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIsNullOrUserId(UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
