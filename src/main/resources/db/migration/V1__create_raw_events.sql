CREATE EXTENSION IF NOT EXISTS timescaledb;

DO $$
BEGIN
    CREATE TABLE IF NOT EXISTS raw_events (
      id UUID,
      received_at timestamptz NOT NULL,
      payload text NOT NULL,
      classification jsonb,
      PRIMARY KEY (id, received_at)
    );

    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
      PERFORM create_hypertable('raw_events', 'received_at', if_not_exists => TRUE);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_raw_events_classification_gin
ON raw_events USING GIN (classification);