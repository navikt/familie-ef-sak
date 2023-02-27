/**
  View som returnerer alle gjeldende iverksatte behandlinger
  Husk å filtrere på stønadstype ved behov
 */
DROP VIEW gjeldende_iverksatte_behandlinger; -- Må slette pga nye kolonner i behandling
CREATE OR REPLACE VIEW gjeldende_iverksatte_behandlinger AS
SELECT *
FROM (SELECT f.stonadstype,
             ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.vedtakstidspunkt DESC) rn,
             f.migrert,
             f.fagsak_person_id,
             b.*
      FROM behandling b
               JOIN fagsak f ON b.fagsak_id = f.id
      WHERE b.resultat IN ('OPPHØRT', 'INNVILGET')
        AND b.status = 'FERDIGSTILT') q
WHERE rn = 1;