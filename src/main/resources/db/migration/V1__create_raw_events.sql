-- filepath: src/main/resources/db/migration/V1__create_raw_events.sql
-- Flyway migration to create raw_events table and convert to hypertable for TimescaleDB

-- Create extension only if it's available on the server. Some PG distributions (or Testcontainer images)
-- may not have the timescaledb extension installed; skip gracefully in that case.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
    -- create extension if available
    EXECUTE 'CREATE EXTENSION IF NOT EXISTS timescaledb';
  ELSE
    RAISE NOTICE 'timescaledb extension not available; skipping CREATE EXTENSION';
  END IF;
END$$;

-- Create raw_events in a Timescale-aware way: when create_hypertable exists, avoid creating a
-- unique primary key that does not include the partitioning column (received_at). Instead,
-- create the table without a single-column PK, create the hypertable, then add a composite
-- primary key (id, received_at). If Timescale is not available, create a normal table with id PK.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
    -- timescaledb available: create table (if not exists) without single-column PK
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'raw_events') THEN
      EXECUTE $create$
        CREATE TABLE raw_events (
          id UUID,
          received_at timestamptz NOT NULL,
          module text NOT NULL,
          temperature double precision NOT NULL,
          humidity double precision NOT NULL,
          pressure double precision NOT NULL
        );
      $create$;
    END IF;

    -- create hypertable (if not exists)
    PERFORM create_hypertable('raw_events', 'received_at', if_not_exists => TRUE);

    -- ensure composite primary key (id, received_at) exists
    IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conrelid = 'raw_events'::regclass AND contype = 'p'
    ) THEN
      EXECUTE 'ALTER TABLE raw_events ADD PRIMARY KEY (id, received_at)';
    END IF;
  ELSE
    -- timescaledb not available: create plain table with id as primary key (if not exists)
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'raw_events') THEN
      EXECUTE $create$
        CREATE TABLE raw_events (
          id UUID PRIMARY KEY,
          received_at timestamptz NOT NULL,
          module text NOT NULL,
          temperature double precision NOT NULL,
          humidity double precision NOT NULL,
          pressure double precision NOT NULL
        );
      $create$;
    END IF;
  END IF;
END$$;
