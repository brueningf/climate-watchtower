package com.audit.climate.watchtower.processing;

import com.audit.climate.watchtower.preprocess.CanonicalEvent;

public interface EventProcessor {
    /**
     * Whether this processor supports a given channel (routing key / queue) or event type.
     * Implementations can choose how to match (exact channel, eventType, etc.).
     */
    boolean supports(CanonicalEvent event);

    /**
     * Process the canonical event (e.g., evaluate thresholds and emit alerts).
     */
    void process(CanonicalEvent event);
}

