package com.audit.climate.watchman.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TimescaleInitializer {
    private static final Logger log = LoggerFactory.getLogger(TimescaleInitializer.class);
    private final JdbcTemplate jdbc;

    public TimescaleInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            log.info("Running TimescaleDB dev initializer (creating extension/table if missing)");

            // enable extension (safe to run repeatedly)
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS timescaledb;");

            // create table if missing (schema matches Flyway migration)
            jdbc.execute("CREATE TABLE IF NOT EXISTS raw_events (\n"
                    + "  id UUID PRIMARY KEY,\n"
                    + "  received_at timestamptz NOT NULL,\n"
                    + "  payload jsonb NOT NULL\n"
                    + ");");

            // If hypertable record already present, skip
            Integer count = jdbc.queryForObject(
                    "SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_name = 'raw_events'",
                    Integer.class);

            if (count != null && count > 0) {
                log.info("Hypertable 'raw_events' already exists; skipping create_hypertable");
                return;
            }

            // Ensure the partition column exists before trying to create hypertable
            Integer colCount = jdbc.queryForObject(
                    "SELECT count(*) FROM information_schema.columns WHERE table_name='raw_events' AND column_name='received_at'",
                    Integer.class);

            if (colCount == null || colCount == 0) {
                log.warn("Cannot create hypertable: column 'received_at' not found on raw_events; skipping hypertable creation");
                return;
            }

            try {
                jdbc.execute("SELECT create_hypertable('raw_events', 'received_at', if_not_exists => TRUE);");
                log.info("Hypertable 'raw_events' created or ensured");
            } catch (Exception ex) {
                // If create_hypertable fails (e.g., due to existing incompatible index), log and continue
                log.warn("create_hypertable failed; leaving table as-is. Error: {}", ex.getMessage());
            }

            log.info("TimescaleDB dev initializer completed");
        } catch (Exception ex) {
            log.warn("Timescale initializer failed (continuing). If running in CI or without DB this may be expected.", ex);
        }
    }
}
