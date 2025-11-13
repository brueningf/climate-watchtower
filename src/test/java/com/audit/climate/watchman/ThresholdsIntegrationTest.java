package com.audit.climate.watchman;

import com.audit.climate.watchman.alerts.Alert;
import com.audit.climate.watchman.alerts.AlertRepository;
import com.audit.climate.watchman.processing.ThresholdEntry;
import com.audit.climate.watchman.processing.ThresholdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThresholdsIntegrationTest {

    @Autowired
    private ThresholdRepository thresholdRepository;

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
        // Arrange: create threshold for module 'it-test' temperature max 10.0
        ThresholdEntry t = new ThresholdEntry("it-test", "temperature", null, 10.0);
        thresholdRepository.save(t);

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

