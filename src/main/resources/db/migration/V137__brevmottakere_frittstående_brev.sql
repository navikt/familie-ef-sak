CREATE TABLE brevmottakere_frittstaende_brev
(
    id                  UUID PRIMARY KEY,
    fagsak_id           UUID         NOT NULL REFERENCES fagsak (id),
    saksbehandler_ident VARCHAR      NOT NULL,
    tidspunkt_opprettet TIMESTAMP(3) NOT NULL,
    personer            JSON         NOT NULL,
    organisasjoner      JSON         NOT NULL
);