ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN stonad_fom DROP NOT NULL;
ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN stonad_tom DROP NOT NULL;
ALTER TABLE andel_tilkjent_ytelse ALTER COLUMN belop DROP NOT NULL;

ALTER TABLE tilkjent_ytelse ADD COLUMN behandling_ekstern_id BIGINT REFERENCES behandling_ekstern(id);

ALTER TABLE tilkjent_ytelse DROP COLUMN IF EXISTS saksbehandler;
