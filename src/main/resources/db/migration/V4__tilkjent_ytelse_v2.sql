DROP TABLE andel_tilkjent_ytelse;
DROP TABLE tilkjent_ytelse;

CREATE TABLE tilkjent_ytelse (
    id                                          BIGSERIAL           primary key,
    ekstern_id                                  UUID                NOT NULL unique,
    type                                        VARCHAR(50)         NOT NULL,
    status                                      VARCHAR(50)         NOT NULL,
    personIdent                                 VARCHAR(20)         NOT NULL,
    saksnummer                                  VARCHAR(50)         NOT NULL,
    stonad_fom                                  DATE,
    stonad_tom                                  DATE,
    opphor_fom                                  DATE,
    utbetalingsoppdrag                          VARCHAR,
    forrige_tilkjent_ytelse_id_fkey             BIGINT              REFERENCES tilkjent_ytelse (id),
    vedtaksdato                                 DATE,
    opprettet_av                                VARCHAR             NOT NULL DEFAULT 'VL',
    opprettet_tid                               TIMESTAMP(3)        NOT NULL DEFAULT localtimestamp,
    endret_av                                   VARCHAR,
    endret_tid                                  TIMESTAMP(3)
);

CREATE TABLE andel_tilkjent_ytelse (
    id                                  BIGSERIAL           primary key,
    tilkjentytelse_id_fkey              BIGINT              REFERENCES tilkjent_ytelse (id),
    personIdent                         VARCHAR (20)        NOT NULL,
    stonad_fom                          DATE                NOT NULL,
    stonad_tom                          DATE                NOT NULL,
    belop                               BIGINT              NOT NULL,
    type                                VARCHAR (100)       NOT NULL
);
