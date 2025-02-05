CREATE TABLE naeringsinntekt_kontroll(
    id                  UUID PRIMARY KEY,
    oppgave_id          INT,
    fagsak_id           UUID,
    kjoretidspunkt      TIMESTAMP(3),
    utfall              VARCHAR
);