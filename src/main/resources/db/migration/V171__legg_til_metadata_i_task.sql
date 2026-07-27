-- V170 er allerede kjørt i dev/prod, så den skal ikke endres i ettertid (gir sjekksum-avvik i Flyway).
-- Legger derfor til metadata-kolonnen i en ny migrasjon i stedet.
ALTER TABLE prosessering.task
    ADD COLUMN IF NOT EXISTS metadata JSONB;
