// ...existing code...
package com.audit.climate.watchtower.preprocess;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PreprocessorRegistry {
    private final Map<String, EventPreprocessor> map = new HashMap<>();
    private EventPreprocessor fallback;

    public PreprocessorRegistry(List<EventPreprocessor> preprocessors) {
        for (EventPreprocessor p : preprocessors) {
            for (String ch : p.supportedChannels()) {
                if ("*".equals(ch)) {
                    fallback = p;
                } else {
                    map.put(ch, p);
                }
            }
        }
        if (fallback == null && !preprocessors.isEmpty()) {
            fallback = preprocessors.get(0);
        }
    }

    public EventPreprocessor getForChannel(String channel) {
        if (channel != null && map.containsKey(channel)) return map.get(channel);
        return fallback;
    }
}

