package com.audit.climate.watchman.processing;

import com.audit.climate.watchman.alerts.Alert;
import com.audit.climate.watchman.alerts.AlertRepository;
import com.audit.climate.watchman.config.RabbitConfig;
import com.audit.climate.watchman.ingest.WeatherEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDetector {
    private static final Logger log = LoggerFactory.getLogger(AnomalyDetector.class);

    private final ThresholdConfig thresholdConfig;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AlertRepository alertRepository;

    public AnomalyDetector(ThresholdConfig thresholdConfig, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, AlertRepository alertRepository) {
        this.thresholdConfig = thresholdConfig;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.alertRepository = alertRepository;
    }

    @EventListener
    public void onWeatherEvent(WeatherEvent evt) {
        try {
            checkMetric(evt.getModule(), "temperature", evt.getTemperature());
            checkMetric(evt.getModule(), "humidity", evt.getHumidity());
            checkMetric(evt.getModule(), "pressure", evt.getPressure());
        } catch (Exception ex) {
            log.error("Error while processing WeatherEvent", ex);
        }
    }

    private void checkMetric(String module, String metric, double value) {
        ThresholdConfig.Range r = thresholdConfig.getThreshold(module, metric);
        if (r == null) {
            // no configured threshold → nothing to do
            return;
        }

        boolean low = r.min != null && value < r.min;
        boolean high = r.max != null && value > r.max;

        if (low || high) {
            String desc = String.format("%s %s out of range (value=%.2f, min=%s, max=%s)", module, metric, value, r.min, r.max);
            Alert alert = new Alert(module, metric, value, r.min, r.max, desc);

            // persist alert
            try {
                alertRepository.save(alert);
                log.info("Persisted alert {} for module={} metric={} value={}", alert.getId(), module, metric, value);
            } catch (Exception ex) {
                log.error("Failed to persist alert", ex);
            }

            // publish to alerts exchange for other systems
            try {
                String payload = objectMapper.writeValueAsString(alert);
                rabbitTemplate.convertAndSend(RabbitConfig.ALERTS_EXCHANGE, "alerts.routing", payload);
                log.debug("Published alert to rabbit exchange");
            } catch (Exception ex) {
                log.error("Failed to publish alert to RabbitMQ", ex);
            }
        }
    }
}
