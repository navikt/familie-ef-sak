CREATE TABLE behandling_journalpost (
    behandling_id       UUID         REFERENCES behandling (id),
    journalpost_id      VARCHAR      NOT NULL,
    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp,

    unique(behandling_id, journalpost_id)
);