CREATE TABLE IF NOT EXISTS thresholds (
  id UUID PRIMARY KEY,
  module text NOT NULL,
  metric text NOT NULL,
  min double precision,
  max double precision,
  updated_at timestamptz NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS thresholds_module_metric_idx ON thresholds(module, metric);

