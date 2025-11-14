package com.audit.climate.watchtower.processing;

import com.audit.climate.watchtower.preprocess.CanonicalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnomalyDetector {
    private static final Logger log = LoggerFactory.getLogger(AnomalyDetector.class);

    private final List<EventProcessor> processors;

    public AnomalyDetector(List<EventProcessor> processors) {
        this.processors = processors;
    }

    @EventListener
    public void onCanonicalEvent(CanonicalEvent evt) {
        try {
            for (EventProcessor p : processors) {
                try {
                    if (p.supports(evt)) {
                        p.process(evt);
                    }
                } catch (Exception ex) {
                    log.error("Processor {} failed to process event", p.getClass().getSimpleName(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("Error while dispatching CanonicalEvent", ex);
        }
    }
}
