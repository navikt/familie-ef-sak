CREATE TABLE tilbakekreving (
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    valg          VARCHAR      NOT NULL,
    varseltekst   VARCHAR,
    begrunnelse   VARCHAR,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);