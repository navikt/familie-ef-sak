CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS opprettet_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS endret_av VARCHAR NOT NULL DEFAULT 'VL';
ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP;
ALTER TABLE behandling_barn ADD COLUMN IF NOT EXISTS endret_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP;


INSERT INTO behandling_barn(id, behandling_id, navn, person_ident, fodsel_termindato, soknad_barn_id, opprettet_av, endret_av,
                            opprettet_tid, endret_tid)
SELECT uuid_generate_v4(),
       soknad_grunnlag.behandling_id,
       soknad_barn.navn,
       soknad_barn.fodselsnummer,
       soknad_barn.fodsel_termindato,
       soknad_barn.id,
       soknad_grunnlag.opprettet_av,
       soknad_grunnlag.endret_av,
       soknad_grunnlag.opprettet_tid,
       soknad_grunnlag.endret_tid
FROM soknad_barn
         JOIN soknad_grunnlag ON soknad_grunnlag.soknadsskjema_id = soknad_barn.soknadsskjema_id;
