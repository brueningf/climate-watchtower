-- Initialize TimescaleDB extension and create the audit table for dev

CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS raw_events (
  id UUID PRIMARY KEY,
  received_at timestamptz NOT NULL,
  module text NOT NULL,
  temperature double precision NOT NULL,
  humidity double precision NOT NULL,
  pressure double precision NOT NULL
);

-- Make it a hypertable on received_at
SELECT * FROM timescaledb_information.hypertables WHERE hypertable_name = 'raw_events';
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM timescaledb_information.hypertables WHERE hypertable_name = 'raw_events') THEN
    PERFORM create_hypertable('raw_events', 'received_at', if_not_exists => true);
  END IF;
END$$;
