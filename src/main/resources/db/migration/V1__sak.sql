CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE sak (
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    soknad         BYTEA        NOT NULL,
    saksnummer     VARCHAR      NOT NULL,
    journalpost_id VARCHAR      NOT NULL,
    opprettet_av   VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid  TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av      VARCHAR      NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL DEFAULT localtimestamp
);
