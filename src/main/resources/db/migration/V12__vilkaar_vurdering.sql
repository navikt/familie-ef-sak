CREATE TABLE vilkar_vurdering
(
    id                  UUID         PRIMARY KEY,
    behandling_id       UUID         REFERENCES behandling(id),
    resultat            VARCHAR      NOT NULL,
    type                VARCHAR      NOT NULL,
    begrunnelse         VARCHAR,
    unntak              VARCHAR,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp
)