package com.backend.backend;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VectorStoreSmokeTest {

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private Environment environment;

	@Test
	void embedsAndFindsSamePhrase() {
		String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
		Assumptions.assumeTrue(
			!apiKey.isBlank() && !"replace_with_real_openai_key".equals(apiKey),
			"Set SPRING_AI_OPENAI_API_KEY in .env before running this test."
		);

		String phrase = "testing embedding";

		vectorStore.add(List.of(new Document(phrase)));

		List<Document> results = vectorStore.similaritySearch(
			SearchRequest.builder()
				.query(phrase)
				.topK(1)
				.build()
		);

		assertThat(results).isNotNull();
		assertThat(results).isNotEmpty();
		assertThat(results.getFirst().getText()).contains(phrase);
	}
}
