CREATE TABLE terminbarn_oppgave (
    fagsak_id     UUID NOT NULL REFERENCES fagsak (id),
    termindato    DATE NOT NULL,
    opprettet_tid DATE NOT NULL
);

CREATE INDEX ON terminbarn_oppgave (fagsak_id);