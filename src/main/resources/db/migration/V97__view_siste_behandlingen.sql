/**
  View som returnerer alle gjeldende iverksatte behandlinger
  Husk å filtrere på stønadstype ved behov
 */
CREATE OR REPLACE VIEW gjeldende_iverksatte_behandlinger AS
SELECT *
FROM (SELECT b.*,
             f.stonadstype,
             ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.opprettet_tid DESC) rn,
             f.migrert
      FROM behandling b
               JOIN fagsak f ON b.fagsak_id = f.id
      WHERE b.type != 'BLANKETT'
        AND b.resultat IN ('OPPHØRT', 'INNVILGET')
        AND b.status = 'FERDIGSTILT') q
WHERE rn = 1;