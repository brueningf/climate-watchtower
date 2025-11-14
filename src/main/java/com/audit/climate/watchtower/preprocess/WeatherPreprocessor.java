package com.audit.climate.watchtower.preprocess;

import com.audit.climate.watchtower.config.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class WeatherPreprocessor implements EventPreprocessor {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Set<String> supportedChannels() {
        // handle messages sent to the test/default queue (and any other weather queues you register here)
        return Set.of(RabbitConfig.DEFAULT_QUEUE);
    }

    @Override
    public CanonicalEvent preprocess(String rawJson, Message amqpMessage) throws Exception {
        JsonNode node = mapper.readTree(rawJson);
        // event type for weather events can be explicit in payload or default to 'weather'
        String eventType = node.has("type") ? node.get("type").asText() : "weather";

        Instant ts;
        if (node.has("timestamp")) {
            try {
                ts = Instant.parse(node.get("timestamp").asText());
            } catch (Exception ex) {
                ts = Instant.now();
            }
        } else {
            ts = Instant.now();
        }

        Map<String, Object> payload = new HashMap<>();
        if (node.has("module")) payload.put("module", mapper.treeToValue(node.get("module"), Object.class));
        if (node.has("temperature")) payload.put("temperature", mapper.treeToValue(node.get("temperature"), Object.class));
        if (node.has("humidity")) payload.put("humidity", mapper.treeToValue(node.get("humidity"), Object.class));
        if (node.has("pressure")) payload.put("pressure", mapper.treeToValue(node.get("pressure"), Object.class));
        // also include the full parsed JSON payload under 'raw' for convenience
        payload.put("raw", mapper.treeToValue(node, Object.class));

        String channel = amqpMessage.getMessageProperties() != null ? amqpMessage.getMessageProperties().getReceivedRoutingKey() : null;
        if (channel == null && amqpMessage.getMessageProperties() != null) channel = amqpMessage.getMessageProperties().getConsumerQueue();
        if (channel == null) channel = "unknown";

        return new CanonicalEvent(channel, eventType, ts, payload);
    }
}
