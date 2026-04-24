package com.backend.backend.domain.transaction;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    boolean existsByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    @Modifying
    @Query(
            value = "UPDATE transactions SET embedding = CAST(:embedding AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);

    @Query(
            value =
                    "SELECT * FROM transactions"
                            + " WHERE user_id = :userId"
                            + " ORDER BY embedding <=> CAST(:embedding AS vector)"
                            + " LIMIT :limit",
            nativeQuery = true)
    List<Transaction> findSimilarByUserId(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);
}
