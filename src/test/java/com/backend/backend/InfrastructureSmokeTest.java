package com.backend.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "app.env.validation.enabled=false")
class InfrastructureSmokeTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private Environment environment;

    @Autowired private VectorStore vectorStore;

    @Test
    void loadsDatabasePropertiesFromEnvironment() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String datasourceUser = environment.getProperty("spring.datasource.username", "");
        String datasourcePassword = environment.getProperty("spring.datasource.password", "");

        assertThat(datasourceUrl).isNotBlank();
        assertThat(datasourceUrl).startsWith("jdbc:postgresql://");
        assertThat(datasourceUrl).contains("backend-4");
        assertThat(datasourceUser).isNotBlank();
        assertThat(datasourcePassword).isNotBlank();
    }

    @Test
    void databaseIsReachableAndVectorExtensionEnabled() {
        Integer ping = jdbcTemplate.queryForObject("select 1", Integer.class);
        String vectorExtension =
                jdbcTemplate.queryForObject(
                        "select extname from pg_extension where extname = 'vector'", String.class);

        assertThat(ping).isEqualTo(1);
        assertThat(vectorExtension).isEqualTo("vector");
    }

    @Test
    void vectorStoreBeanIsConfigured() {
        assertThat(vectorStore).isNotNull();
    }
}
