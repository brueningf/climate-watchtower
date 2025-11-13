package com.audit.climate.watchtower.processing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thresholds")
public class ThresholdsController {
    private final ThresholdRepository repository;
    private final ThresholdConfig config;

    public ThresholdsController(ThresholdRepository repository, ThresholdConfig config) {
        this.repository = repository;
        this.config = config;
    }

    // List all thresholds
    @GetMapping
    public List<ThresholdDto> list() {
        return repository.findAll().stream().map(ThresholdDto::fromEntity).collect(Collectors.toList());
    }

    // Get a single threshold by module+metric
    @GetMapping(params = {"module", "metric"})
    public ResponseEntity<ThresholdDto> get(@RequestParam String module, @RequestParam String metric) {
        return repository.findByModuleAndMetric(module, metric)
                .map(e -> ResponseEntity.ok(ThresholdDto.fromEntity(e)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create or update a threshold
    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody UpsertRequest req) {
        if (req.getModule() == null || req.getMetric() == null) {
            return ResponseEntity.badRequest().body("module and metric are required");
        }
        if (req.getMin() != null && req.getMax() != null && req.getMin() > req.getMax()) {
            return ResponseEntity.badRequest().body("min must be <= max");
        }
        config.setThreshold(req.getModule(), req.getMetric(), req.getMin(), req.getMax());
        // Return location of resource (convention)
        URI location = URI.create(String.format("/api/thresholds?module=%s&metric=%s", req.getModule(), req.getMetric()));
        return ResponseEntity.created(location).build();
    }

    // Delete a threshold
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String module, @RequestParam String metric) {
        config.deleteThreshold(module, metric);
        return ResponseEntity.noContent().build();
    }

    public static class UpsertRequest {
        private String module;
        private String metric;
        private Double min;
        private Double max;

        public UpsertRequest() {}

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public Double getMin() { return min; }
        public void setMin(Double min) { this.min = min; }
        public Double getMax() { return max; }
        public void setMax(Double max) { this.max = max; }
    }

    public static class ThresholdDto {
        private String module;
        private String metric;
        private Double min;
        private Double max;

        public static ThresholdDto fromEntity(ThresholdEntry e) {
            ThresholdDto d = new ThresholdDto();
            d.module = e.getModule();
            d.metric = e.getMetric();
            d.min = e.getMin();
            d.max = e.getMax();
            return d;
        }

        public String getModule() { return module; }
        public String getMetric() { return metric; }
        public Double getMin() { return min; }
        public Double getMax() { return max; }
    }
}
