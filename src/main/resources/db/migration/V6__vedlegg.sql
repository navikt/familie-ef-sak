CREATE TABLE vedlegg (
    id              UUID PRIMARY KEY,
    sak_id          UUID REFERENCES sak(id),
    opprettet_av    VARCHAR                     NOT NULL DEFAULT 'VL',
    opprettet_tid   TIMESTAMP(3)                NOT NULL DEFAULT localtimestamp,
    endret_av       VARCHAR                     NOT NULL,
    endret_tid      TIMESTAMP(3)                NOT NULL DEFAULT localtimestamp,
    data            BYTEA                       NOT NULL,
    navn            VARCHAR                     NOT NULL
);
