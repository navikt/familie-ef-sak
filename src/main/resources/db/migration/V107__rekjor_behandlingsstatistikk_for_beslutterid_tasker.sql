/**
  Behandling-DVH ønsker å få rekjørt behandlingsstatistikk-tasker
  som er av hendelsetype besluttet for å få med beslutterId og saksbehandlerId i hendelsen
 */
UPDATE task
SET status='KLAR_TIL_PLUKK'
WHERE type = 'behandlingsstatistikkTask'
    AND payload LIKE '%BESLUTTET%'
    AND opprettet_tid > '2022-01-12 11:45:59';