DELETE FROM task_logg tl WHERE task_id IN (SELECT t.id FROM task t WHERE type='forberedOppgaverForBarnTask');
DELETE FROM task where type='forberedOppgaverForBarnTask';