package com.audit.climate.watchtower.alerts;

import com.audit.climate.watchtower.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AlertService is the single entry point for persisting and publishing alerts.
 * Centralizes save/publish logic with proper error handling.
 */
@Service
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public AlertService(AlertRepository alertRepository,
                       RabbitTemplate rabbitTemplate,
                       ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist and publish all alerts.
     * Persists to database first, then publishes to RabbitMQ.
     * Continues on individual failures but logs errors.
     *
     * @param alerts list of alerts to persist and publish
     */
    public void persistAndPublish(List<Alert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (Alert alert : alerts) {
            try {
                // Persist to database
                alertRepository.save(alert);
                log.info("Persisted alert {} for module={} metric={} value={}",
                    alert.getId(), alert.getModule(), alert.getMetric(), alert.getValue());

                // Publish to RabbitMQ
                try {
                    String payload = objectMapper.writeValueAsString(alert);
                    rabbitTemplate.convertAndSend(
                        RabbitConfig.ALERTS_EXCHANGE,
                        "alerts.routing",
                        payload
                    );
                    log.debug("Published alert {} to RabbitMQ", alert.getId());
                } catch (Exception ex) {
                    log.error("Failed to publish alert {} to RabbitMQ", alert.getId(), ex);
                }
            } catch (Exception ex) {
                log.error("Failed to persist alert for module={} metric={}",
                    alert.getModule(), alert.getMetric(), ex);
            }
        }
    }
}

