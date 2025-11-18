package com.audit.climate.watchtower.detection;

import com.audit.climate.watchtower.alerts.Alert;
import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DetectorRegistry discovers and runs all registered detectors.
 * Collects all alerts from all detectors.
 */
@Component
public class DetectorRegistry {
    private static final Logger log = LoggerFactory.getLogger(DetectorRegistry.class);

    private final List<Detector> detectors;

    public DetectorRegistry(List<Detector> detectors) {
        this.detectors = detectors;
        log.info("DetectorRegistry initialized with {} detectors", detectors.size());
    }

    /**
     * Run all detectors on the given canonical event.
     * @param event the canonical event
     * @return list of all alerts from all detectors
     */
    public List<Alert> runDetectors(CanonicalEvent event) {
        List<Alert> allAlerts = new ArrayList<>();

        for (Detector detector : detectors) {
            try {
                List<Alert> alerts = detector.detect(event);
                if (alerts != null && !alerts.isEmpty()) {
                    allAlerts.addAll(alerts);
                }
            } catch (Exception ex) {
                log.error("Detector {} failed", detector.getClass().getSimpleName(), ex);
            }
        }

        return allAlerts;
    }
}

