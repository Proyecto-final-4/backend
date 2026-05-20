package com.backend.backend;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "app.env.validation.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EnvironmentStartupValidator implements ApplicationRunner {

    private final Environment environment;

    public EnvironmentStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        validateRequired("SPRING_DATASOURCE_URL", "spring.datasource.url", errors);
        validateRequired("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", errors);
        validateRequired("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", errors);
        validateRequired("SPRING_AI_OPENAI_API_KEY", "spring.ai.openai.api-key", errors);
        validateRequired("SPRING_CLOUD_AWS_ENDPOINT", "spring.cloud.aws.endpoint", errors);
        validateRequired(
                "SPRING_CLOUD_AWS_REGION_STATIC", "spring.cloud.aws.region.static", errors);
        validateRequired("APP_ENCRYPTION_KEY", "app.encryption.key", errors);

        String openAiApiKey = environment.getProperty("spring.ai.openai.api-key", "");
        if ("replace_with_real_openai_key".equals(openAiApiKey)) {
            errors.add("SPRING_AI_OPENAI_API_KEY uses a placeholder value.");
        }

        String encryptionKey = environment.getProperty("app.encryption.key", "");
        if ("replace_with_base64_32bytes_key".equals(encryptionKey)) {
            errors.add(
                    "APP_ENCRYPTION_KEY uses a placeholder value. Generate one with: openssl rand"
                            + " -base64 32");
        } else if (!encryptionKey.isBlank()) {
            try {
                byte[] keyBytes = java.util.Base64.getDecoder().decode(encryptionKey);
                if (keyBytes.length != 32) {
                    errors.add(
                            "APP_ENCRYPTION_KEY must decode to exactly 32 bytes (256 bits). Got "
                                    + keyBytes.length
                                    + " bytes.");
                }
            } catch (IllegalArgumentException e) {
                errors.add("APP_ENCRYPTION_KEY is not valid Base64.");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Environment validation failed. Fix the following values before starting the"
                            + " server: "
                            + String.join(" | ", errors));
        }
    }

    private void validateRequired(String envName, String propertyName, List<String> errors) {
        String value = environment.getProperty(propertyName, "").trim();
        if (value.isBlank()) {
            errors.add(envName + " (mapped from " + propertyName + ") is missing.");
        }
    }
}
