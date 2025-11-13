package com.audit.climate.watchman;

import com.audit.climate.watchman.alerts.Alert;
import com.audit.climate.watchman.alerts.AlertRepository;
import com.audit.climate.watchman.processing.ThresholdConfig;
import com.audit.climate.watchman.processing.ThresholdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThresholdsIntegrationTest {

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
    private ThresholdRepository thresholdRepository;

    @Autowired
    private ThresholdConfig thresholdConfig;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AlertRepository alertRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    public void cleanup() {
        thresholdRepository.deleteAll();
        alertRepository.deleteAll();
    }

    @Test
    public void whenThresholdSetAndEventOutOfRange_thenAlertIsCreated() throws Exception {
        // Arrange: set threshold for module 'it-test' temperature max 10.0 via ThresholdConfig so in-memory map is updated
        thresholdConfig.setThreshold("it-test", "temperature", null, 10.0);

        // Build a WeatherEvent-like payload with temperature=20 (out of range)
        Map<String, Object> payload = Map.of(
                "module", "it-test",
                "temperature", 20.0,
                "humidity", 50.0,
                "pressure", 750.0
        );
        String json = objectMapper.writeValueAsString(payload);

        // Act: send to default queue (test.queue)
        rabbitTemplate.convertAndSend("test.queue", json);

        // Wait up to 5 seconds for alert to appear
        boolean found = false;
        for (int i = 0; i < 25; i++) {
            List<Alert> alerts = alertRepository.findAll();
            Optional<Alert> any = alerts.stream()
                    .filter(a -> "it-test".equals(a.getModule()) && "temperature".equals(a.getMetric()))
                    .findAny();
            if (any.isPresent()) {
                found = true;
                break;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }

        Assertions.assertTrue(found, "Expected an alert to be created for out-of-range temperature");
    }
}
