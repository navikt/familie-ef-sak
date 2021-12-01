CREATE TABLE uttrekk_arbeidssoker (
    id              UUID PRIMARY KEY,
    fagsak_id       UUID         NOT NULL,
    vedtak_id       UUID         NOT NULL,
    aar_maaned      DATE         NOT NULL,
    opprettet_tid   TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    kontrollert     BOOLEAN      NOT NULL,
    kontrollert_av  VARCHAR,
    kontrollert_tid TIMESTAMP
);

CREATE INDEX ON uttrekk_arbeidssoker (aar_maaned);