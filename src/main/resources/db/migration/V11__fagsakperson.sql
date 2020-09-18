CREATE TABLE fagsak_person (
    id                  UUID         PRIMARY KEY,
    fagsak_id           UUID         REFERENCES fagsak (id),
    ident               VARCHAR      NOT NULL,
    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp

);