package com.backend.backend.domain.transaction;

import com.backend.backend.shared.CurrentUserService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final TransactionRepository transactionRepository;
    private final EmbeddingService embeddingService;
    private final CurrentUserService currentUserService;

    public RagService(
            TransactionRepository transactionRepository,
            EmbeddingService embeddingService,
            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.embeddingService = embeddingService;
        this.currentUserService = currentUserService;
    }

    public List<RagSearchResponse> search(String email, RagSearchRequest request) {
        var user = currentUserService.resolve(email);

        float[] embedding = embeddingService.generateEmbedding(request.query());
        String embeddingVector = toVectorString(embedding);

        return transactionRepository
                .findSimilarByUserId(user.getId(), embeddingVector, request.limit())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String toVectorString(float[] embedding) {
        String values =
                Arrays.stream(toFloatObjectArray(embedding))
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
        return "[" + values + "]";
    }

    private Float[] toFloatObjectArray(float[] primitive) {
        Float[] boxed = new Float[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            boxed[i] = primitive[i];
        }
        return boxed;
    }

    private RagSearchResponse toResponse(Transaction transaction) {
        return new RagSearchResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getNotes(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getTransactionDate(),
                transaction.getCategory().getName());
    }
}
