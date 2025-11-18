package com.audit.climate.watchtower.canonical;

import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Canonicalizer converts raw JSON messages into CanonicalEvent.
 * Single deterministic transformation - no optional behavior.
 */
@Component
public class Canonicalizer {
    private final ObjectMapper mapper;

    public Canonicalizer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Canonicalize a raw JSON payload into a CanonicalEvent.
     * Extracts channel from AMQP message, event type from payload or defaults to "weather",
     * timestamp from payload or defaults to now, and payload fields.
     */
    public CanonicalEvent canonicalize(String rawJson, Message amqpMessage) throws Exception {
        JsonNode node = mapper.readTree(rawJson);

        // Extract event type (default: "weather")
        String eventType = node.has("type") ? node.get("type").asText() : "weather";

        // Extract timestamp (default: now)
        Instant timestamp;
        if (node.has("timestamp")) {
            try {
                timestamp = Instant.parse(node.get("timestamp").asText());
            } catch (Exception ex) {
                timestamp = Instant.now();
            }
        } else {
            timestamp = Instant.now();
        }

        // Extract channel from AMQP message
        String channel = extractChannel(amqpMessage);

        // Build payload map
        Map<String, Object> payload = new HashMap<>();
        if (node.has("module")) payload.put("module", mapper.treeToValue(node.get("module"), Object.class));
        if (node.has("temperature")) payload.put("temperature", mapper.treeToValue(node.get("temperature"), Object.class));
        if (node.has("humidity")) payload.put("humidity", mapper.treeToValue(node.get("humidity"), Object.class));
        if (node.has("pressure")) payload.put("pressure", mapper.treeToValue(node.get("pressure"), Object.class));

        return new CanonicalEvent(channel, eventType, timestamp, payload);
    }

    private String extractChannel(Message amqpMessage) {
        if (amqpMessage.getMessageProperties() != null) {
            String channel = amqpMessage.getMessageProperties().getReceivedRoutingKey();
            if (channel != null) return channel;
            channel = amqpMessage.getMessageProperties().getConsumerQueue();
            if (channel != null) return channel;
        }
        return "unknown";
    }
}

