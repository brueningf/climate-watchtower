package com.audit.climate.watchtower.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "raw_events")
public class RawEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    // original raw payload stored verbatim as text (JSON string)
    @Lob
    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    // classification/normalized metadata stored as JSONB (mapped as String)
    @Column(name = "classification", columnDefinition = "jsonb")
    private String classification;

    // JPA requires a no-arg constructor
    protected RawEvent() { }

    public RawEvent(String payload) {
        this.payload = payload;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.receivedAt == null) this.receivedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getPayload() { return payload; }
    public String getClassification() { return classification; }

    public void setPayload(String payload) { this.payload = payload; }
    public void setClassification(String classification) { this.classification = classification; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawEvent)) return false;
        RawEvent rawEvent = (RawEvent) o;
        return Objects.equals(id, rawEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RawEvent{" +
                "id=" + id +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
