CREATE TABLE konsistensavstemming_jobb (
    id          SERIAL PRIMARY KEY,
    triggerdato DATE     NOT NULL,
    versjon     SMALLINT NOT NULL DEFAULT 1,
    opprettet   BOOLEAN  NOT NULL DEFAULT FALSE
)