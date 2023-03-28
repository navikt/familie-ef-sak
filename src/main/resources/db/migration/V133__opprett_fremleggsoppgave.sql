CREATE TABLE opprett_fremleggsoppgave
(
    behandling_id UUID PRIMARY KEY NOT NULL,
    oppgavetyper  VARCHAR[] NOT NULL
);