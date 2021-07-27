with vedtakquery as (SELECT behandling_id FROM vedtak WHERE resultat_type = 'INNVILGE')
UPDATE behandling
SET resultat = 'INNVILGET'
FROM vedtakquery
WHERE behandling.id = vedtakquery.behandling_id AND behandling.status = 'FERDIGSTILT';


