CREATE TABLE arsak_revurdering
(
    behandling_id    UUID PRIMARY KEY references behandling (id),
    opplysningskilde VARCHAR      NOT NULL,
    arsak            VARCHAR      NOT NULL,
    beskrivelse      VARCHAR,
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL DEFAULT localtimestamp
)