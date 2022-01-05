ALTER TABLE vedtak
    DROP COLUMN brevmottakere;

CREATE TABLE brevmottakere (
    behandling_id  UUID PRIMARY KEY REFERENCES behandling (id),
    personer       JSON,
    organisasjoner JSON
);