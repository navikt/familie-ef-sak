ALTER TABLE vedtak
    ADD COLUMN opprettet_tid TIMESTAMP(3);
ALTER TABLE vedtak
    ADD COLUMN opprettet_av VARCHAR;
-- Oppdaterer behandlinger som er henlagt eller sendt til totrinnskontroll
UPDATE vedtak
SET saksbehandler_ident = COALESCE(saksbehandler_ident, bh.opprettet_av),
    opprettet_tid       = COALESCE(opprettet_tid, bh.endret_tid),
    opprettet_av        = bh.opprettet_av
FROM (SELECT *, row_number() OVER (PARTITION BY behandling_id ORDER BY endret_tid DESC) rn
      FROM behandlingshistorikk bh
      WHERE bh.steg = 'BEREGNE_YTELSE') bh
WHERE vedtak.behandling_id = bh.behandling_id
  AND bh.rn = 1;

UPDATE vedtak
SET saksbehandler_ident = COALESCE(saksbehandler_ident, b.opprettet_av),
    opprettet_tid       = COALESCE(opprettet_tid, b.vedtakstidspunkt),
    opprettet_av        = b.opprettet_av
FROM (SELECT * FROM behandling WHERE arsak IN ('MIGRERING', 'G_OMREGNING')) b
WHERE vedtak.behandling_id = b.id;

ALTER TABLE vedtak
    ALTER COLUMN opprettet_tid SET NOT NULL;
ALTER TABLE vedtak
    ALTER COLUMN saksbehandler_ident SET NOT NULL;
ALTER TABLE vedtak
    ALTER COLUMN opprettet_av SET NOT NULL;

-- VEDTAKSBREV

ALTER table vedtaksbrev
    ADD COLUMN opprettet_tid TIMESTAMP(3);
ALTER TABLE vedtaksbrev
    ADD COLUMN besluttet_tid TIMESTAMP(3);
UPDATE vedtaksbrev
SET opprettet_tid = COALESCE(opprettet_tid, bh.endret_tid)
FROM (SELECT *, row_number() OVER (PARTITION BY behandling_id ORDER BY endret_tid DESC) rn
      FROM behandlingshistorikk bh
      WHERE bh.steg = 'SEND_TIL_BESLUTTER') bh
WHERE vedtaksbrev.behandling_id = bh.behandling_id
  AND bh.rn = 1;

UPDATE vedtaksbrev
SET besluttet_tid = COALESCE(opprettet_tid, bh.endret_tid)
FROM (SELECT *, row_number() OVER (PARTITION BY behandling_id ORDER BY endret_tid DESC) rn
      FROM behandlingshistorikk bh
      WHERE bh.steg = 'BESLUTTE_VEDTAK' AND bh.utfall = 'BESLUTTE_VEDTAK_GODKJENT') bh
WHERE vedtaksbrev.behandling_id = bh.behandling_id
  AND bh.rn = 1;

