CREATE TABLE uttrekk_arbeidssoker (
    id            UUID PRIMARY KEY,
    fagsak_id     UUID         NOT NULL,
    vedtak_id     UUID         NOT NULL,
    aar_maaned    DATE         NOT NULL,
    kontrollert       BOOLEAN,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);

CREATE INDEX ON uttrekk_arbeidssoker (aar_maaned);