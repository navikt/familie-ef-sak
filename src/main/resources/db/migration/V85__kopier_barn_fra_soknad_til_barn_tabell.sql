CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
INSERT INTO behandling_barn(id, behandling_id, navn, personident, fodsel_termindato, soknad_barn_id)
SELECT uuid_generate_v4(),
       grunnlag_soknad.behandling_id,
       soknad_barn.navn,
       soknad_barn.fodselsnummer,
       soknad_barn.fodsel_termindato,
       soknad_barn.id
FROM soknad_barn
         JOIN grunnlag_soknad ON grunnlag_soknad.soknadsskjema_id = soknad_barn.soknadsskjema_id;

-- Oppdater barn_id på vilkårsvurderingene
UPDATE vilkarsvurdering
SET barn_id = subquery.id
FROM (SELECT id, behandling_id, soknad_barn_id::TEXT FROM behandling_barn) AS subquery
WHERE vilkarsvurdering.behandling_id = subquery.behandling_id
  AND vilkarsvurdering.barn_id = subquery.soknad_barn_id;