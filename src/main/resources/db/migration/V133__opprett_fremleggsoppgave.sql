CREATE TABLE oppgave_for_opprettelse
(
    behandling_id UUID PRIMARY KEY NOT NULL,
    oppgavetyper  VARCHAR[] NOT NULL
);