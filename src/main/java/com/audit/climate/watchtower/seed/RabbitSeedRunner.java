package com.audit.climate.watchtower.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.rabbit.seed.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitSeedRunner {
    private static final Logger log = LoggerFactory.getLogger(RabbitSeedRunner.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random rnd = new Random();

    @Value("${app.rabbit.seed.exchange:}") // empty = default exchange
    private String exchange;

    @Value("${app.rabbit.seed.routing-key:test.queue}")
    private String routingKey;

    @Value("${app.rabbit.seed.count:100}")
    private int count;

    // mode can be 'once' or 'continuous' (endless loop)
    @Value("${app.rabbit.seed.mode:continuous}")
    private String mode;

    // interval between messages when in continuous mode (ms)
    @Value("${app.rabbit.seed.interval-ms:1000}")
    private long intervalMs;

    // comma-separated list of modules to choose from
    @Value("${app.rabbit.seed.modules:green-medium}")
    private String modules;

    private String[] modulesArray;

    public RabbitSeedRunner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // parse modules into array once
        if (modules != null && !modules.isBlank()) {
            modulesArray = modules.split("\\s*,\\s*");
        } else {
            modulesArray = new String[]{"green-medium"};
        }

        log.info("ApplicationReadyEvent received — starting seeder in background thread (mode={}).", mode);
        Thread t = new Thread(() -> {
            try {
                if ("once".equalsIgnoreCase(mode)) {
                    sendCount(count);
                    log.info("Seeder finished 'once' mode; leaving application running.");
                    return;
                }

                long sent = 0L;
                while (!Thread.currentThread().isInterrupted()) {
                    sendOne(sent + 1);
                    sent++;
                    if (sent % 10 == 0) log.info("Sent {} messages (continuous mode)", sent);
                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                log.info("Seeder background thread exiting after sending {} messages.", sent);
            } catch (Throwable t1) {
                log.error("Seeder background thread terminated with error", t1);
            }
        }, "seed-runner-thread");
        t.setDaemon(true);
        t.start();
    }

    private void sendCount(int cnt) {
        for (int i = 0; i < cnt; i++) {
            sendOne(i + 1);
            if ((i + 1) % 10 == 0) log.info("Sent {} messages", i + 1);
            try {
                Thread.sleep(20);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendOne(long id) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", id);
            msg.put("type", "telemetry");

            // Choose a module from the configured list
            String module = modulesArray[rnd.nextInt(modulesArray.length)];
            msg.put("module", module);

            msg.put("deviceId", "dev-" + (1000 + rnd.nextInt(9000)));
            msg.put("temperature", 15 + rnd.nextDouble() * 20);
            msg.put("humidity", 20 + rnd.nextDouble() * 60);
            msg.put("pressure", 700 + rnd.nextDouble() * 50);
            msg.put("ts", Instant.now().toString());

            String payload = objectMapper.writeValueAsString(msg);
            if (exchange == null || exchange.isEmpty()) {
                rabbitTemplate.convertAndSend(routingKey, payload); // default exchange -> routingKey is queue name
            } else {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            }
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
        }
    }
}
