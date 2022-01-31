CREATE TABLE behandling_barn (
    id                UUID PRIMARY KEY,
    behandling_id     UUID NOT NULL REFERENCES behandling (id),
    soknad_barn_id    UUID,
    navn              VARCHAR,
    personident       VARCHAR,
    fodsel_termindato DATE
);

CREATE INDEX ON behandling_barn (behandling_id);