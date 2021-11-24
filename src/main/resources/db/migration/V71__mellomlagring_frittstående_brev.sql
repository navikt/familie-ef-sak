CREATE TABLE mellomlagret_frittstaende_brev (
    id                  UUID PRIMARY KEY            NOT NULL,
    fagsak_id           UUID REFERENCES fagsak (id) NOT NULL,
    brev                JSON                        NOT NULL,
    brev_type           VARCHAR                     NOT NULL,
    saksbehandler_ident VARCHAR                     NOT NULL,
    tidspunkt_opprettet TIMESTAMP(3)                NOT NULL,
    UNIQUE (fagsak_id, saksbehandler_ident)
);

ALTER TABLE mellomlagret_fritekstbrev
    ADD COLUMN brev_type VARCHAR;
UPDATE mellomlagret_fritekstbrev
SET brev_type = 'VEDTAK_INVILGELSE'
