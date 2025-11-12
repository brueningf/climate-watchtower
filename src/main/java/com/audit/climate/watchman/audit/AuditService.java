package com.audit.climate.watchman.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final RawEventRepository repository;

    public AuditService(RawEventRepository repository) {
        this.repository = repository;
    }

    public void persistRawEvent(String jsonPayload) {
        try {
            RawEvent e = new RawEvent(jsonPayload);
            repository.save(e);
            log.debug("Persisted raw event id={}", e.getId());
        } catch (Exception ex) {
            log.error("Failed to persist raw event", ex);
            throw ex;
        }
    }
}

