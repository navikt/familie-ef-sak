DELETE
FROM task_logg
WHERE task_id IN (SELECT id
             FROM task
             WHERE id = 6929
               AND type = 'opprettUttrekkArbeidssøker'
               AND status = 'UBEHANDLET');
DELETE
FROM task
WHERE id = 6929
  AND type = 'opprettUttrekkArbeidssøker'
  AND status = 'UBEHANDLET';