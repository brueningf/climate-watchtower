package com.audit.climate.watchman.audit;

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

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "temperature", nullable = false)
    private double temperature;

    @Column(name = "humidity", nullable = false)
    private double humidity;

    @Column(name = "pressure", nullable = false)
    private double pressure;

    public RawEvent() {
        this.id = UUID.randomUUID();
        this.receivedAt = Instant.now();
    }

    public RawEvent(String module, double temperature, double humidity, double pressure) {
        this();
        this.module = module;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
    }

    public UUID getId() {
        return id;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getModule() {
        return module;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }
}
