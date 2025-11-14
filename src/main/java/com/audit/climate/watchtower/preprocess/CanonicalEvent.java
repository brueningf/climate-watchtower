package com.audit.climate.watchtower.preprocess;

import java.time.Instant;
import java.util.Map;

public class CanonicalEvent {
    private final String channel;
    private final String eventType;
    private final Instant timestamp;
    private final Map<String, Object> payload;

    public CanonicalEvent(String channel, String eventType, Instant timestamp, Map<String, Object> payload) {
        this.channel = channel;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getPayload() { return payload; }
}

