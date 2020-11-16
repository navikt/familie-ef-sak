ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN stonad_fom DROP NOT NULL;
ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN stonad_tom DROP NOT NULL;
ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN belop DROP NOT NULL;

ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS opprettet_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS endret_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp;
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS endret_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp;
