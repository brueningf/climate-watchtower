package com.audit.climate.watchman.alerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "value", nullable = false)
    private double value;

    @Column(name = "threshold_min")
    private Double thresholdMin;

    @Column(name = "threshold_max")
    private Double thresholdMax;

    @Column(name = "description")
    private String description;

    public Alert() {
        this.id = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public Alert(String module, String metric, double value, Double thresholdMin, Double thresholdMax, String description) {
        this();
        this.module = module;
        this.metric = metric;
        this.value = value;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getModule() {
        return module;
    }

    public String getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public Double getThresholdMin() {
        return thresholdMin;
    }

    public Double getThresholdMax() {
        return thresholdMax;
    }

    public String getDescription() {
        return description;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setThresholdMin(Double thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public void setThresholdMax(Double thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

