CREATE TABLE utestengelse
(
    id               UUID PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    fom              DATE         NOT NULL,
    tom              DATE         NOT NULL,
    slettet          BOOLEAN      NOT NULL,
    opprettet_av     VARCHAR      NOT NULL,
    opprettet_tid    TIMESTAMP(3) NOT NULL,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP(3) NOT NULL
);

CREATE INDEX ON utestengelse (fagsak_person_id);