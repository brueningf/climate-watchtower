-- filepath: src/main/resources/db/migration/V1__create_raw_events.sql

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
    EXECUTE 'CREATE EXTENSION IF NOT EXISTS timescaledb';
  ELSE
    RAISE NOTICE 'timescaledb extension not available; skipping CREATE EXTENSION';
  END IF;
END$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'raw_events') THEN
      EXECUTE $create$
        CREATE TABLE raw_events (
          id UUID,
          received_at timestamptz NOT NULL,
          payload text NOT NULL,
          classification text,
          PRIMARY KEY (id, received_at)
        );
      $create$;
    END IF;

    PERFORM create_hypertable('raw_events', 'received_at', if_not_exists => TRUE);
  ELSE
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'raw_events') THEN
      EXECUTE $create$
        CREATE TABLE raw_events (
          id UUID PRIMARY KEY,
          received_at timestamptz NOT NULL,
          payload text NOT NULL,
          classification text
        );
      $create$;
    END IF;
  END IF;

  -- create indexes that help queries on classification
  IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename='raw_events' AND indexname='idx_raw_events_classification') THEN
    EXECUTE 'CREATE INDEX idx_raw_events_classification ON raw_events (classification)';
  END IF;
END$$;
