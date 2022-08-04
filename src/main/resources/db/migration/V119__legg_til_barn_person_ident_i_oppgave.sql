ALTER TABLE oppgave ADD COLUMN barn_person_ident VARCHAR;
ALTER TABLE oppgave ADD COLUMN alder VARCHAR;
DELETE FROM task where type='forberedOppgaverForBarnTask';