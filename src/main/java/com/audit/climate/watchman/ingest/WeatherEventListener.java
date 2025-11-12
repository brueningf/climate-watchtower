package com.audit.climate.watchman.ingest;

import com.audit.climate.watchman.audit.AuditService;
import com.audit.climate.watchman.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class WeatherEventListener {
    private static final Logger log = LoggerFactory.getLogger(WeatherEventListener.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;

    public WeatherEventListener(AuditService auditService, ObjectMapper objectMapper, ApplicationEventPublisher publisher) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    /**
     * Listen for raw JSON messages on the configured queue, persist them for audit,
     * attempt to deserialize into {@link WeatherEvent} and publish as an application event
     * so downstream processing can consume it.
     */
    @RabbitListener(queues = {RabbitConfig.DEFAULT_QUEUE})
    public void onMessage(String payload) {
        log.debug("Received raw message: {}", payload);

        // Try to parse into typed event and publish for processing
        try {
            WeatherEvent event = objectMapper.readValue(payload, WeatherEvent.class);
            log.debug("Parsed WeatherEvent: {}", event);

            try {
                String module = event.getModule();
                if (module == null || module.isBlank()) {
                    module = "unknown";
                }
                auditService.persistRawEvent(module, event.getTemperature(), event.getHumidity(), event.getPressure());
            } catch (Exception ex) {
                log.error("Failed to persist raw event", ex);
            }

            publisher.publishEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to deserialize payload into WeatherEvent; skipping processing publish", ex);
        }
    }
}
