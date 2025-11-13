package com.audit.climate.watchman.alerts;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AlertsController {
    private final AlertRepository alertRepository;

    public AlertsController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public List<Alert> listAlerts() {
        return alertRepository.findAll().stream().collect(Collectors.toList());
    }
}
