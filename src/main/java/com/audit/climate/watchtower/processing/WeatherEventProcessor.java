package com.audit.climate.watchtower.processing;

import com.audit.climate.watchtower.alerts.Alert;
import com.audit.climate.watchtower.alerts.AlertRepository;
import com.audit.climate.watchtower.config.RabbitConfig;
import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherEventProcessor implements EventProcessor {
    private static final Logger log = LoggerFactory.getLogger(WeatherEventProcessor.class);

    private final ThresholdConfig thresholdConfig;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AlertRepository alertRepository;

    public WeatherEventProcessor(ThresholdConfig thresholdConfig,
                                 RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper,
                                 AlertRepository alertRepository) {
        this.thresholdConfig = thresholdConfig;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.alertRepository = alertRepository;
    }

    @Override
    public boolean supports(CanonicalEvent event) {
        String eventType = event.getEventType();
        String channel = event.getChannel();
        return "weather".equalsIgnoreCase(eventType) || (channel != null && channel.equals(RabbitConfig.DEFAULT_QUEUE));
    }

    @Override
    public void process(CanonicalEvent evt) {
        try {
            Map<String,Object> p = evt.getPayload();
            if (p == null) return;

            String module = null;
            Object mid = p.get("module");
            if (mid != null) module = mid.toString();
            if (module == null || module.isBlank()) module = evt.getChannel();

            if (p.containsKey("temperature")) checkMetric(module, "temperature", toDouble(p.get("temperature")));
            if (p.containsKey("humidity")) checkMetric(module, "humidity", toDouble(p.get("humidity")));
            if (p.containsKey("pressure")) checkMetric(module, "pressure", toDouble(p.get("pressure")));
        } catch (Exception ex) {
            log.error("Error while processing weather CanonicalEvent", ex);
        }
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception ex) { return Double.NaN; }
    }

    private void checkMetric(String module, String metric, double value) {
        if (Double.isNaN(value)) return;
        ThresholdConfig.Range r = thresholdConfig.getThreshold(module, metric);
        if (r == null) return;

        boolean low = r.min != null && value < r.min;
        boolean high = r.max != null && value > r.max;

        if (low || high) {
            String desc = String.format("%s %s out of range (value=%.2f, min=%s, max=%s)", module, metric, value, r.min, r.max);
            Alert alert = new Alert(module, metric, value, r.min, r.max, desc);

            try {
                alertRepository.save(alert);
                log.info("Persisted alert {} for module={} metric={} value={}", alert.getId(), module, metric, value);
            } catch (Exception ex) {
                log.error("Failed to persist alert", ex);
            }

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

