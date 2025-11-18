DO $$
BEGIN
    CREATE TABLE IF NOT EXISTS alerts (
      id UUID NOT NULL,
      occurred_at timestamptz NOT NULL,
      module text NOT NULL,
      metric text NOT NULL,
      value double precision NOT NULL,
      threshold_min double precision,
      threshold_max double precision,
      description text,
      PRIMARY KEY (id, occurred_at)
    );

    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_hypertable') THEN
      PERFORM create_hypertable('alerts', 'occurred_at', if_not_exists => TRUE);
    END IF;
END$$;
