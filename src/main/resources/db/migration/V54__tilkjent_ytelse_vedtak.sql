ALTER TABLE tilkjent_ytelse ADD COLUMN vedtakstidspunkt TIMESTAMP(3);
UPDATE tilkjent_ytelse SET vedtakstidspunkt = endret_tid;
ALTER TABLE tilkjent_ytelse ALTER COLUMN vedtakstidspunkt SET NOT NULL;
ALTER TABLE tilkjent_ytelse DROP COLUMN vedtaksdato;