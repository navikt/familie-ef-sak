ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS opprettet_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS endret_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp;
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS endret_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp;
