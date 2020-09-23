CREATE TABLE fagsak_person (
    fagsak_id           UUID         REFERENCES fagsak (id),
    ident               VARCHAR      NOT NULL,
    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp,

    unique(fagsak_id, ident)
);