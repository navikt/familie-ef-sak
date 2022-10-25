CREATE INDEX ON tilkjent_ytelse(behandling_id);

ALTER TABLE behandling ADD COLUMN vedtakstidspunkt TIMESTAMP(3);