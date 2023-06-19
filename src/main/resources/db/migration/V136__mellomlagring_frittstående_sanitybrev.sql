CREATE TABLE mellomlagret_frittstaende_sanitybrev
(
    id                  UUID PRIMARY KEY            NOT NULL,
    fagsak_id           UUID REFERENCES fagsak (id) NOT NULL,
    brevverdier         VARCHAR                     NOT NULL,
    brevmal             VARCHAR                     NOT NULL,
    saksbehandler_ident VARCHAR                     NOT NULL,
    opprettet_tid       TIMESTAMP(3)                NOT NULL,
    UNIQUE (fagsak_id, saksbehandler_ident)
);