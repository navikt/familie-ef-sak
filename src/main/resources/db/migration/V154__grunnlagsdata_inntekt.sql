CREATE TABLE grunnlagsdata_inntekt
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    inntektsdata json NOT NULL,
    oppdaterte_inntektsdata json,
    oppdaterte_inntektsdata_hentet_tid TIMESTAMP(3),
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT localtimestamp
);