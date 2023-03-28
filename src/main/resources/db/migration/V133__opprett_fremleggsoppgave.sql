CREATE TABLE opprett_fremleggsoppgave
(
    behandling_id UUID PRIMARY KEY NOT NULL,
    oppgave_type  VARCHAR[]          NOT NULL
);