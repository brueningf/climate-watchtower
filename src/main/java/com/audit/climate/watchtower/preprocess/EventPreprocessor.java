// ...existing code...
package com.audit.climate.watchtower.preprocess;

import org.springframework.amqp.core.Message;

import java.util.Set;

public interface EventPreprocessor {
    Set<String> supportedChannels();
    CanonicalEvent preprocess(String rawJson, Message amqpMessage) throws Exception;
}

