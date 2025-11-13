package com.audit.climate.watchman.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thresholds", uniqueConstraints = {@UniqueConstraint(columnNames = {"module", "metric"})})
public class ThresholdEntry {

    @Id
    private UUID id;

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "min")
    private Double min;

    @Column(name = "max")
    private Double max;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ThresholdEntry() {
        this.id = UUID.randomUUID();
        this.updatedAt = Instant.now();
    }

    public ThresholdEntry(String module, String metric, Double min, Double max) {
        this();
        this.module = module;
        this.metric = metric;
        this.min = min;
        this.max = max;
    }

    public UUID getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}

