CREATE TABLE mellomlagret_fritstaende_brev (
    id                  UUID PRIMARY KEY            NOT NULL,
    fagsak_id           UUID REFERENCES fagsak (id) NOT NULL,
    brev                JSON                        NOT NULL,
    brev_type           VARCHAR                     NOT NULL,
    saksbehandler_ident VARCHAR                     NOT NULL,
    tidspunkt_opprettet TIMESTAMP(3)                NOT NULL,
    UNIQUE (fagsak_id, saksbehandler_ident)
)