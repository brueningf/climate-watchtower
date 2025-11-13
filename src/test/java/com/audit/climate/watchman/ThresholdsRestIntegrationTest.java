package com.audit.climate.watchman;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Arrays;
import java.util.Map;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThresholdsRestIntegrationTest {

    @Container
    // Use a Timescale-enabled Postgres image so the timescaledb extension is available in tests
    public static PostgreSQLContainer<?> postgres;

    static {
        DockerImageName image = DockerImageName.parse("timescale/timescaledb:2.9.0-pg15")
                .asCompatibleSubstituteFor("postgres");
        postgres = new PostgreSQLContainer<>(image)
                .withDatabaseName("watchman_audit")
                .withUsername("postgres")
                .withPassword("postgres");
    }

    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);

        // configure rabbit properties
        reg.add("spring.rabbitmq.host", rabbit::getHost);
        reg.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort());
        reg.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        reg.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void crudLifecycle_andValidation() throws Exception {
        // Create
        Map<String,Object> body = Map.of(
                "module", "rest-test",
                "metric", "temperature",
                "min", 1.0,
                "max", 5.0
        );

        ResponseEntity<Void> created = restTemplate.postForEntity("/api/thresholds", body, Void.class);
        Assertions.assertEquals(201, created.getStatusCodeValue());

        // Get
        ResponseEntity<ThresholdDto> get = restTemplate.getForEntity("/api/thresholds?module=rest-test&metric=temperature", ThresholdDto.class);
        Assertions.assertEquals(200, get.getStatusCodeValue());
        Assertions.assertNotNull(get.getBody());
        Assertions.assertEquals("rest-test", get.getBody().getModule());
        Assertions.assertEquals("temperature", get.getBody().getMetric());
        Assertions.assertEquals(1.0, get.getBody().getMin());
        Assertions.assertEquals(5.0, get.getBody().getMax());

        // List
        ResponseEntity<ThresholdDto[]> list = restTemplate.getForEntity("/api/thresholds", ThresholdDto[].class);
        Assertions.assertEquals(200, list.getStatusCodeValue());
        Assertions.assertNotNull(list.getBody());
        boolean found = Arrays.stream(list.getBody())
                .anyMatch(d -> "rest-test".equals(d.getModule()) && "temperature".equals(d.getMetric()));
        Assertions.assertTrue(found);

        // Validation: min > max should return 400
        Map<String,Object> bad = Map.of(
                "module", "rest-test-2",
                "metric", "temperature",
                "min", 10.0,
                "max", 1.0
        );
        ResponseEntity<String> badResp = restTemplate.postForEntity("/api/thresholds", bad, String.class);
        Assertions.assertEquals(400, badResp.getStatusCodeValue());

        // Delete
        restTemplate.delete("/api/thresholds?module=rest-test&metric=temperature");
        ResponseEntity<ThresholdDto> afterDelete = restTemplate.getForEntity("/api/thresholds?module=rest-test&metric=temperature", ThresholdDto.class);
        Assertions.assertEquals(404, afterDelete.getStatusCodeValue());
    }

    // DTO mapping for responses
    public static class ThresholdDto {
        private String module;
        private String metric;
        private Double min;
        private Double max;

        public String getModule() { return module; }
        public String getMetric() { return metric; }
        public Double getMin() { return min; }
        public Double getMax() { return max; }

        public void setModule(String module) { this.module = module; }
        public void setMetric(String metric) { this.metric = metric; }
        public void setMin(Double min) { this.min = min; }
        public void setMax(Double max) { this.max = max; }
    }
}

