CREATE TABLE terminbarnoppgave (
    fagsak_id     UUID,
    termindato    TIMESTAMP(3) NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);