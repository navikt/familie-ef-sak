UPDATE task t
SET status= 'KLAR_TIL_PLUKK'
WHERE type = 'startBehandlingTask'
  AND status = 'FERDIG'
  AND EXISTS(SELECT * FROM behandling WHERE id = t.payload AND status = 'FERDIGSTILT');