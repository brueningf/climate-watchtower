package com.audit.climate.watchtower.ingest;

import com.audit.climate.watchtower.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class MultiQueueAuditListener {
    private static final Logger log = LoggerFactory.getLogger(MultiQueueAuditListener.class);

    private final SimpleMessageListenerContainer container;
    private final AuditService auditService;
    private final String[] queues;

    public MultiQueueAuditListener(AuditService auditService,
                                   ConnectionFactory connectionFactory,
                                   @Value("${app.rabbit.audit.queues:test.queue}") String queuesCsv) {
        this.auditService = auditService;

        // parse configured queues (comma separated)
        this.queues = Arrays.stream(queuesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("Configuring audit listener for queues: {}", String.join(",", queues));

        this.container = new SimpleMessageListenerContainer(connectionFactory);
        this.container.setQueueNames(this.queues);
        // do not fail the application startup if a queue is missing; we'll log and continue
        this.container.setMissingQueuesFatal(false);

        this.container.setMessageListener((org.springframework.amqp.core.Message message) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("Received raw message on multi-queue listener: {}", payload);

            try {
                String channel = null;
                if (message.getMessageProperties() != null) {
                    channel = message.getMessageProperties().getReceivedRoutingKey();
                    if (channel == null) channel = message.getMessageProperties().getConsumerQueue();
                }
                if (channel == null) channel = "unknown";
                auditService.persistRawEvent(channel, payload, message);
            } catch (Exception ex) {
                log.error("Failed to persist raw event", ex);
            }
        });

        // do NOT start the container here; start it after application is ready so RabbitAdmin can declare queues
        this.container.setAutoStartup(false);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startContainer() {
        log.info("Starting audit listener for queues: {}", String.join(",", queues));
        try {
            this.container.start();
        } catch (Exception ex) {
            log.warn("Failed to start message listener container cleanly; continuing. Error: {}", ex.getMessage());
        }
    }
}
