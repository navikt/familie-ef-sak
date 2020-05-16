DROP TABLE andel_tilkjent_ytelse;
DROP TABLE tilkjent_ytelse;

CREATE TABLE tilkjent_ytelse (
    id                                          BIGSERIAL          primary key,
    ekstern_id                                  UUID               unique,
    personIdent                                 VARCHAR(20),
    saksnummer                                  VARCHAR(50),
    stonad_fom                                  DATE,
    stonad_tom                                  DATE,
    opphor_fom                                  DATE,
    utbetalingsoppdrag                          VARCHAR,
    forrige_tilkjent_ytelse_id_fkey             BIGINT,
    vedtaksdato                                 DATE,
    status                                      VARCHAR(50),
    opprettet_av                                VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid                               TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av                                   VARCHAR,
    endret_tid                                  TIMESTAMP(3)
);

CREATE TABLE andel_tilkjent_ytelse (
    id                                  BIGSERIAL          primary key,
    tilkjentytelse_id_fkey              BIGINT             REFERENCES tilkjent_ytelse (id),
    personIdent                         VARCHAR (20),
    stonad_fom                          DATE,
    stonad_tom                          DATE,
    belop                               BIGINT,
    type                                VARCHAR (100)
);
