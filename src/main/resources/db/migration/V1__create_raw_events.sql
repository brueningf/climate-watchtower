-- filepath: src/main/resources/db/migration/V1__create_raw_events.sql
-- Flyway migration to create raw_events table and convert to hypertable for TimescaleDB

CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS raw_events (
  id UUID PRIMARY KEY,
  received_at timestamptz NOT NULL,
  module text NOT NULL,
  temperature double precision NOT NULL,
  humidity double precision NOT NULL,
  pressure double precision NOT NULL
);

-- create hypertable if available
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
    PERFORM create_hypertable('raw_events', 'received_at', if_not_exists => TRUE);
  END IF;
END$$;
