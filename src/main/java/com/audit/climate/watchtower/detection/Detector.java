package com.audit.climate.watchtower.detection;

import com.audit.climate.watchtower.alerts.Alert;
import com.audit.climate.watchtower.preprocess.CanonicalEvent;

import java.util.List;

public interface Detector {
    List<Alert> detect(CanonicalEvent event);
}

