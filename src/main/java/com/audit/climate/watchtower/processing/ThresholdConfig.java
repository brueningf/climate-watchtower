package com.audit.climate.watchtower.processing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThresholdConfig now persists thresholds to the DB using ThresholdRepository.
 */
@Component
public class ThresholdConfig {
    private static final Logger log = LoggerFactory.getLogger(ThresholdConfig.class);

    // keys: module:metric (e.g. "green-medium:temperature")
    private final Map<String, Range> thresholds = new ConcurrentHashMap<>();

    private final ThresholdRepository repository;

    public ThresholdConfig(ThresholdRepository repository) {
        this.repository = repository;
    }

    public static class Range {
        public final Double min;
        public final Double max;

        public Range(Double min, Double max) {
            this.min = min;
            this.max = max;
        }
    }

    @PostConstruct
    public void loadFromDb() {
        try {
            repository.findAll().forEach(e -> {
                thresholds.put(key(e.getModule(), e.getMetric()), new Range(e.getMin(), e.getMax()));
            });
            log.info("Loaded {} thresholds from DB", thresholds.size());
        } catch (Exception ex) {
            log.error("Failed to load thresholds from DB", ex);
        }
    }

    public void setThreshold(String module, String metric, Double min, Double max) {
        String k = key(module, metric);
        thresholds.put(k, new Range(min, max));

        try {
            ThresholdEntry entry = repository.findByModuleAndMetric(module, metric)
                    .orElseGet(() -> new ThresholdEntry(module, metric, min, max));
            entry.setMin(min);
            entry.setMax(max);
            entry.touch();
            repository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist threshold to DB", ex);
        }
    }

    public void deleteThreshold(String module, String metric) {
        String k = key(module, metric);
        thresholds.remove(k);
        try {
            repository.findByModuleAndMetric(module, metric).ifPresent(repository::delete);
        } catch (Exception ex) {
            log.error("Failed to delete threshold from DB", ex);
        }
    }

    public Range getThreshold(String module, String metric) {
        return thresholds.get(key(module, metric));
    }

    private String key(String module, String metric) {
        return module + ":" + metric;
    }
}
