package com.backend.backend.domain.budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Budget> findByIdAndUser_Id(UUID id, UUID userId);
}
