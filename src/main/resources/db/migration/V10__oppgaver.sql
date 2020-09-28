CREATE TABLE oppgave (
    id                  UUID            PRIMARY KEY,
    behandling_id       UUID            REFERENCES behandling (id),
    gsak_oppgave_id     BIGINT          NOT NULL,
    type                VARCHAR         NOT NULL,
    ferdigstilt         BOOLEAN         NOT NULL,
    opprettet_av        VARCHAR         NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3)    NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR         NOT NULL,
    endret_tid          TIMESTAMP(3)    NOT NULL DEFAULT localtimestamp
);
