package com.audit.climate.watchtower.ingest;

import com.audit.climate.watchtower.alerts.Alert;
import com.audit.climate.watchtower.alerts.AlertService;
import com.audit.climate.watchtower.audit.RawEvent;
import com.audit.climate.watchtower.audit.RawEventRepository;
import com.audit.climate.watchtower.canonical.Canonicalizer;
import com.audit.climate.watchtower.detection.DetectorRegistry;
import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MultiQueueAuditListener orchestrates the complete pipeline:
 * 1. Persist raw event
 * 2. Canonicalize into CanonicalEvent
 * 3. Run detectors to get alerts
 * 4. Persist and publish alerts
 */
@Component
public class MultiQueueAuditListener {
    private static final Logger log = LoggerFactory.getLogger(MultiQueueAuditListener.class);

    private final SimpleMessageListenerContainer container;
    private final RawEventRepository rawEventRepository;
    private final Canonicalizer canonicalizer;
    private final DetectorRegistry detectorRegistry;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;
    private final String[] queues;

    public MultiQueueAuditListener(RawEventRepository rawEventRepository,
                                   Canonicalizer canonicalizer,
                                   DetectorRegistry detectorRegistry,
                                   AlertService alertService,
                                   ObjectMapper objectMapper,
                                   ConnectionFactory connectionFactory,
                                   @Value("${audit.queues:test.queue}") String queuesCsv) {
        this.rawEventRepository = rawEventRepository;
        this.canonicalizer = canonicalizer;
        this.detectorRegistry = detectorRegistry;
        this.alertService = alertService;
        this.objectMapper = objectMapper;

        this.queues = Arrays.stream(queuesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("Configuring audit listener for queues: {}", String.join(",", queues));

        this.container = new SimpleMessageListenerContainer(connectionFactory);
        this.container.setQueueNames(this.queues);
        this.container.setMissingQueuesFatal(false);

        this.container.setMessageListener((org.springframework.amqp.core.Message message) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("Received raw message: {}", payload);

            try {
                // Step 1: Persist raw event
                RawEvent rawEvent = new RawEvent(payload);
                rawEventRepository.save(rawEvent);
                log.debug("Persisted raw event id={}", rawEvent.getId());

                // Step 2: Canonicalize
                CanonicalEvent canonicalEvent;
                try {
                    canonicalEvent = canonicalizer.canonicalize(payload, message);

                    // Store classification metadata
                    Map<String, Object> classification = new HashMap<>();
                    classification.put("eventType", canonicalEvent.getEventType());
                    classification.put("channel", canonicalEvent.getChannel());
                    classification.put("timestamp", canonicalEvent.getTimestamp().toString());
                    classification.put("payloadKeys", canonicalEvent.getPayload().keySet());
                    rawEvent.setClassification(objectMapper.writeValueAsString(classification));
                    rawEventRepository.save(rawEvent);
                } catch (Exception ex) {
                    log.error("Failed to canonicalize raw event {}", rawEvent.getId(), ex);
                    rawEvent.setClassification(objectMapper.writeValueAsString(
                        Map.of("status", "canonicalization_failed", "message", ex.getMessage())
                    ));
                    rawEventRepository.save(rawEvent);
                    return;
                }

                // Step 3: Run detectors
                List<Alert> alerts = detectorRegistry.runDetectors(canonicalEvent);

                // Step 4: Persist and publish alerts
                alertService.persistAndPublish(alerts);

            } catch (Exception ex) {
                log.error("Failed to process message", ex);
            }
        });

        // Start the container
        container.setAutoStartup(true);
        container.start();
        log.info("Started audit listener for queues: {}", String.join(",", queues));
    }
}

