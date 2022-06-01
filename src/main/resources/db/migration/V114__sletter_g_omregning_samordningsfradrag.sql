/**
  Opprettet g-omregning for alle med samordningsfradrag som ikke skulle blitt opprettet
  Det ble kun opprettet behandlingshistorikk og behandling for disse
 */
WITH q AS (SELECT id behandling_id
           FROM behandling b
           WHERE arsak = 'G_OMREGNING' AND resultat = 'IKKE_SATT' AND opprettet_av = 'VL')
DELETE
FROM behandlingshistorikk
WHERE behandling_id IN (SELECT behandling_id FROM q);

WITH q AS (SELECT id behandling_id
           FROM behandling b
           WHERE arsak = 'G_OMREGNING' AND resultat = 'IKKE_SATT' AND opprettet_av = 'VL')
DELETE
FROM behandling_ekstern
WHERE behandling_id IN (SELECT behandling_id FROM q);

DELETE
FROM behandling
WHERE arsak = 'G_OMREGNING'
  AND resultat = 'IKKE_SATT'
  AND opprettet_av = 'VL';