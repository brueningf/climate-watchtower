package com.audit.climate.watchtower.audit;

import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import com.audit.climate.watchtower.preprocess.PreprocessorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final RawEventRepository repository;
    private final ObjectMapper mapper;
    private final PreprocessorRegistry registry;
    private final ApplicationEventPublisher publisher;

    public AuditService(RawEventRepository repository, ObjectMapper mapper, PreprocessorRegistry registry, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.registry = registry;
        this.publisher = publisher;
    }

    public void persistRawEvent(String channel, String rawJson, org.springframework.amqp.core.Message amqpMessage) {
        try {
            RawEvent e = new RawEvent(rawJson);
            repository.save(e);
            log.debug("Persisted raw event id={}", e.getId());

            // attempt preprocessing and publish canonical event for downstream processing
            try {
                var preprocessor = registry.getForChannel(channel);
                CanonicalEvent canonical = preprocessor.preprocess(rawJson, amqpMessage);
                // attach some classification metadata to audit row (as JSON string)
                Map<String,Object> meta = new HashMap<>();
                meta.put("eventType", canonical.getEventType());
                meta.put("channel", canonical.getChannel());
                meta.put("timestamp", canonical.getTimestamp().toString());
                meta.put("preprocessor", preprocessor.getClass().getSimpleName());
                meta.put("payloadKeys", canonical.getPayload().keySet());
                e.setClassification(mapper.writeValueAsString(meta));
                repository.save(e);

                // publish canonical event so processors (anomaly detector) can react
                publisher.publishEvent(canonical);
            } catch (Exception ex) {
                log.error("Preprocessing failed for saved raw event {}, continuing", e.getId(), ex);
                try {
                    e.setClassification(mapper.writeValueAsString(Map.of("status", "preprocess_failed", "message", ex.getMessage())));
                    repository.save(e);
                } catch (Exception ex2) {
                    log.error("Failed to persist classification for raw event {}", e.getId(), ex2);
                }
            }

        } catch (Exception ex) {
            log.error("Failed to persist raw event", ex);
            throw new RuntimeException(ex);
        }
    }
}
