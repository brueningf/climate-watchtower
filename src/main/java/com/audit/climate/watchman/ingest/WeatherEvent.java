package com.audit.climate.watchman.ingest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class WeatherEvent {
    private final String module;
    private final double temperature;
    private final double humidity;
    private final double pressure;

    @JsonCreator
    public WeatherEvent(@JsonProperty("module") String module,
                        @JsonProperty("temperature") double temperature,
                        @JsonProperty("humidity") double humidity,
                        @JsonProperty("pressure") double pressure) {
        this.module = module;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
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

    @Override
    public String toString() {
        return "WeatherEvent{" +
                "module='" + module + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", pressure=" + pressure +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherEvent that = (WeatherEvent) o;
        return Double.compare(that.temperature, temperature) == 0 && Double.compare(that.humidity, humidity) == 0 && Double.compare(that.pressure, pressure) == 0 && Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, temperature, humidity, pressure);
    }
}

