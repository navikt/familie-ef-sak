ALTER TABLE vedtak
    DROP COLUMN brevmottakere;

ALTER TABLE behandling
    ADD COLUMN brevmottakere json;