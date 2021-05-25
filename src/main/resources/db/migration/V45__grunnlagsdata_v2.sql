CREATE TABLE grunnlagsdata (
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    data          JSON         NOT NULL,

    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
)