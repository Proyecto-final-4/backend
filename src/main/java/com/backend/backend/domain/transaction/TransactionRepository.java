package com.backend.backend.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category.id = :categoryId
              AND t.type = com.backend.backend.domain.transaction.TransactionType.EXPENSE
              AND t.transactionDate BETWEEN :from AND :to
            """)
    BigDecimal sumExpenseAmountByUserCategoryAndDateRange(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            """
            SELECT t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.type
            """)
    List<Object[]> sumAmountByTypeForUser(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            """
            SELECT t.category.id, t.category.name, t.type, SUM(t.amount), COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category.id, t.category.name, t.type
            """)
    List<Object[]> sumAmountByCategoryAndTypeForUser(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

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
