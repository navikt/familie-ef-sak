CREATE TABLE oppgaver_for_opprettelse
(
    behandling_id UUID PRIMARY KEY NOT NULL,
    oppgavetyper VARCHAR[] NOT NULL,
    opprettelse_tatt_stilling_til BOOLEAN NOT NULL
);