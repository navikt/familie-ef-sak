CREATE TABLE fremleggsoppgave
(
    id                      UUID PRIMARY KEY NOT NULL,
    behandling_id           UUID             NOT NULL,
    opprettFremleggsoppgave BOOLEAN          NOT NULL
);