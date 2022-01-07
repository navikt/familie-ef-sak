CREATE OR REPLACE VIEW sist_iverksatte_behandling AS
SELECT *
FROM (SELECT b.*,
             f.stonadstype,
             ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.opprettet_tid DESC) rn
      FROM behandling b
               JOIN fagsak f ON b.fagsak_id = f.id
      WHERE b.type != 'BLANKETT'
        AND b.resultat IN ('OPPHØRT', 'INNVILGET')
        AND b.status = 'FERDIGSTILT') q
WHERE rn = 1;