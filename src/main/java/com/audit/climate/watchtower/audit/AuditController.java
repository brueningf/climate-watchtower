package com.audit.climate.watchtower.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final RawEventRepository repository;
    private final ObjectMapper mapper;

    public AuditController(RawEventRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
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
        List<AuditDto> items = p.stream().map(e -> AuditDto.fromEntity(e, mapper)).collect(Collectors.toList());
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
        private String channel;
        private String module;
        private Double temperature;
        private Double humidity;
        private Double pressure;

        public static AuditDto fromEntity(RawEvent e, ObjectMapper mapper) {
            AuditDto d = new AuditDto();
            d.id = e.getId().toString();
            d.receivedAt = e.getReceivedAt().toString();
            try {
                // first attempt: try to extract channel from classification (set by AuditService during preprocessing)
                String cls = e.getClassification();
                if (cls != null) {
                    Map<String,Object> clsMap = mapper.readValue(cls, Map.class);
                    Object ch = clsMap.get("channel");
                    if (ch != null) d.channel = ch.toString();
                }

                String payloadStr = e.getPayload();
                if (payloadStr != null) {
                    Map<String,Object> payload = mapper.readValue(payloadStr, Map.class);

                    // fallback: if channel not in classification, check payload for channel key
                    if (d.channel == null) {
                        Object ch2 = payload.get("channel");
                        if (ch2 != null) d.channel = ch2.toString();
                    }

                    // module is derived from payload
                    Object mid = payload.get("module");
                    if (mid != null) d.module = mid.toString();

                    Object t = payload.get("temperature");
                    if (t instanceof Number) d.temperature = ((Number)t).doubleValue();
                    else if (t != null) d.temperature = Double.parseDouble(t.toString());

                    Object h = payload.get("humidity");
                    if (h instanceof Number) d.humidity = ((Number)h).doubleValue();
                    else if (h != null) d.humidity = Double.parseDouble(h.toString());

                    Object pval = payload.get("pressure");
                    if (pval instanceof Number) d.pressure = ((Number)pval).doubleValue();
                    else if (pval != null) d.pressure = Double.parseDouble(pval.toString());
                }
            } catch (Exception ex) {
                // ignore parsing errors and return null metrics
            }
            return d;
        }

        public String getId() { return id; }
        public String getReceivedAt() { return receivedAt; }
        public String getChannel() { return channel; }
        public String getModule() { return module; }
        public Double getTemperature() { return temperature; }
        public Double getHumidity() { return humidity; }
        public Double getPressure() { return pressure; }
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
