ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS person_ident VARCHAR default null;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS periode_id BIGINT default null;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN IF NOT EXISTS forrige_periode_id BIGINT default null;
ALTER TABLE tilkjent_ytelse DROP COLUMN IF EXISTS periode_id_start;
ALTER TABLE tilkjent_ytelse DROP COLUMN IF EXISTS forrige_periode_id_start;
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS behandling_id BIGINT default null;
ALTER TABLE tilkjent_ytelse ADD COLUMN IF NOT EXISTS saksbehandler VARCHAR default null; 