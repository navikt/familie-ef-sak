UPDATE behandling
SET vedtakstidspunkt = subquery.ny_vedtakstidspunkt
FROM (SELECT b.id
           , coalesce(
            bh.endret_tid,
            CASE WHEN b.resultat = 'HENLAGT' THEN b.endret_tid END,
            CASE WHEN b.arsak = 'MIGRERING' OR b.arsak = 'G_OMREGNING' THEN ty.vedtakstidspunkt END
        ) ny_vedtakstidspunkt
      FROM behandling b
               LEFT JOIN tilkjent_ytelse ty ON ty.behandling_id = b.id
               LEFT JOIN behandlingshistorikk bh
                         ON bh.behandling_id = b.id AND bh.steg = 'BESLUTTE_VEDTAK' AND
                            (metadata ->> 'godkjent') = 'true'
      WHERE b.resultat <> 'IKKE_SATT'
        AND b.vedtakstidspunkt IS NULL
     ) subquery
WHERE subquery.id = behandling.id;

ALTER TABLE behandling
    ADD CONSTRAINT behandling_resultat_vedtakstidspunkt_check
        CHECK ((resultat = 'IKKE_SATT' AND vedtakstidspunkt IS null)
            OR
               (resultat <> 'IKKE_SATT' AND vedtakstidspunkt IS NOT null));