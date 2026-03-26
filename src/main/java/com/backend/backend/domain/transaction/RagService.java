package com.backend.backend.domain.transaction;

import com.backend.backend.domain.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final TransactionRepository transactionRepository;
    private final EmbeddingService embeddingService;
    private final UserRepository userRepository;

    public RagService(
            TransactionRepository transactionRepository,
            EmbeddingService embeddingService,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.embeddingService = embeddingService;
        this.userRepository = userRepository;
    }

    public List<RagSearchResponse> search(String email, RagSearchRequest request) {
        var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

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
