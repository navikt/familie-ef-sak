-- Oppdater barn_id på vilkårsvurderingene - ikke bruk søknad.barn.id
UPDATE vilkarsvurdering
SET barn_id = subquery.id
FROM (SELECT id, behandling_id, soknad_barn_id::TEXT FROM behandling_barn) AS subquery
WHERE vilkarsvurdering.behandling_id = subquery.behandling_id
  AND vilkarsvurdering.barn_id = subquery.soknad_barn_id AND false=true;