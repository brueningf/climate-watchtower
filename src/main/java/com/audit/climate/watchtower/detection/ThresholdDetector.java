package com.audit.climate.watchtower.detection;

import com.audit.climate.watchtower.alerts.Alert;
import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import com.audit.climate.watchtower.processing.ThresholdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ThresholdDetector checks weather metrics against configured thresholds.
 * Converted from WeatherEventProcessor - now pure detector with no persistence logic.
 */
@Component
public class ThresholdDetector implements Detector {
    private static final Logger log = LoggerFactory.getLogger(ThresholdDetector.class);

    private final ThresholdConfig thresholdConfig;

    public ThresholdDetector(ThresholdConfig thresholdConfig) {
        this.thresholdConfig = thresholdConfig;
    }

    @Override
    public List<Alert> detect(CanonicalEvent event) {
        List<Alert> alerts = new ArrayList<>();

        try {
            Map<String, Object> payload = event.getPayload();
            if (payload == null) {
                return alerts;
            }

            // Determine module
            String module = extractModule(payload, event.getChannel());

            // Check each metric
            if (payload.containsKey("temperature")) {
                checkMetric(module, "temperature", toDouble(payload.get("temperature")), alerts);
            }
            if (payload.containsKey("humidity")) {
                checkMetric(module, "humidity", toDouble(payload.get("humidity")), alerts);
            }
            if (payload.containsKey("pressure")) {
                checkMetric(module, "pressure", toDouble(payload.get("pressure")), alerts);
            }
        } catch (Exception ex) {
            log.error("Error in ThresholdDetector", ex);
        }

        return alerts;
    }

    private String extractModule(Map<String, Object> payload, String channel) {
        Object moduleObj = payload.get("module");
        if (moduleObj != null) {
            String module = moduleObj.toString();
            if (!module.isBlank()) {
                return module;
            }
        }
        return channel != null ? channel : "unknown";
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    private void checkMetric(String module, String metric, double value, List<Alert> alerts) {
        if (Double.isNaN(value)) {
            return;
        }

        ThresholdConfig.Range range = thresholdConfig.getThreshold(module, metric);
        if (range == null) {
            return;
        }

        boolean low = range.min != null && value < range.min;
        boolean high = range.max != null && value > range.max;

        if (low || high) {
            String description = String.format(
                "%s %s out of range (value=%.2f, min=%s, max=%s)",
                module, metric, value, range.min, range.max
            );
            Alert alert = new Alert(module, metric, value, range.min, range.max, description);
            alerts.add(alert);
            log.debug("Detected threshold violation: {}", description);
        }
    }
}

