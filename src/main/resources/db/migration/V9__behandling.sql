CREATE TABLE behandling (
    id                  UUID         PRIMARY KEY,
    fagsak_id           UUID         REFERENCES FAGSAK (id),
    versjon             INT          DEFAULT 0,
    aktiv               BOOLEAN      NOT NULL,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp,

    type                VARCHAR      NOT NULL,
    opprinnelse         VARCHAR      NOT NULL,
    status              VARCHAR      NOT NULL,
    steg                VARCHAR      NOT NULL
);