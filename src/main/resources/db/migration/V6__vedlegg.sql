CREATE TABLE vedlegg (
    id              UUID PRIMARY KEY,
    sak_id          UUID REFERENCES sak(id),
    opprettet_tid   TIMESTAMP(3)                NOT NULL    DEFAULT localtimestamp,
    data            BYTEA                       NOT NULL,
    navn            VARCHAR                     NOT NULL
);
