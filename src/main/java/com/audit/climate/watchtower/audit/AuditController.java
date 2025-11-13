// ...existing code...
package com.audit.climate.watchtower.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final RawEventRepository repository;

    public AuditController(RawEventRepository repository) {
        this.repository = repository;
    }

    // GET /api/audit?page=0&size=20&sort=receivedAt,desc
    @GetMapping
    public ResponseEntity<AuditPageDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedAt,desc") String sort
    ) {
        // parse sort param (e.g., receivedAt,desc)
        String[] sortParts = sort.split(",");
        String sortProp = sortParts[0];
        Sort.Direction dir = Sort.Direction.DESC;
        if (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])) {
            dir = Sort.Direction.ASC;
        }
        PageRequest pr = PageRequest.of(page, size, Sort.by(dir, sortProp));
        Page<RawEvent> p = repository.findAll(pr);
        List<AuditDto> items = p.stream().map(AuditDto::fromEntity).collect(Collectors.toList());
        AuditPageDto dto = new AuditPageDto();
        dto.setPage(p.getNumber());
        dto.setSize(p.getSize());
        dto.setTotalElements(p.getTotalElements());
        dto.setTotalPages(p.getTotalPages());
        dto.setItems(items);
        return ResponseEntity.ok(dto);
    }

    public static class AuditDto {
        private String id;
        private String receivedAt;
        private String module;
        private double temperature;
        private double humidity;
        private double pressure;

        public static AuditDto fromEntity(RawEvent e) {
            AuditDto d = new AuditDto();
            d.id = e.getId().toString();
            d.receivedAt = e.getReceivedAt().toString();
            d.module = e.getModule();
            d.temperature = e.getTemperature();
            d.humidity = e.getHumidity();
            d.pressure = e.getPressure();
            return d;
        }

        public String getId() { return id; }
        public String getReceivedAt() { return receivedAt; }
        public String getModule() { return module; }
        public double getTemperature() { return temperature; }
        public double getHumidity() { return humidity; }
        public double getPressure() { return pressure; }
    }

    public static class AuditPageDto {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private List<AuditDto> items;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public List<AuditDto> getItems() { return items; }
        public void setItems(List<AuditDto> items) { this.items = items; }
    }
}

