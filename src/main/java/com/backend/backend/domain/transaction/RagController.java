package com.backend.backend.domain.transaction;

import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/search")
    public List<RagSearchResponse> search(@RequestBody RagSearchRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ragService.search(email, request);
    }
}
