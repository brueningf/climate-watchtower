package com.audit.climate.watchtower.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_events")
public class RawEvent {

    @Id
    private UUID id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    // original raw payload stored verbatim as text (JSON string)
    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    // classification/normalized metadata stored as text (JSON string)
    @Column(name = "classification", columnDefinition = "text")
    private String classification;

    public RawEvent() {
        this.id = UUID.randomUUID();
        this.receivedAt = Instant.now();
    }

    public RawEvent(String payload) {
        this();
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getPayload() { return payload; }
    public String getClassification() { return classification; }

    public void setPayload(String payload) { this.payload = payload; }
    public void setClassification(String classification) { this.classification = classification; }
}
