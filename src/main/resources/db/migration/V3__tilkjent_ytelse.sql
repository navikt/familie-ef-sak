CREATE TABLE tilkjent_ytelse (
    id                                  BIGSERIAL          primary key,
    personIdent                         VARCHAR (20),
    stonad_fom                          TIMESTAMP(3),
    stonad_tom                          TIMESTAMP(3),
    opphor_fom                          TIMESTAMP(3),
    utbetalingsoppdrag                  VARCHAR,
    forrige_tilkjentytelse_id_fkey      bigint,
    saksnummer                          VARCHAR(50),
    vedtaksdato                         TIMESTAMP(3),
    status                              VARCHAR (50)
);

CREATE TABLE andel_tilkjent_ytelse (
    id                                  BIGSERIAL          primary key,
    tilkjentytelse_id_fkey              bigint,            REFERENCES tilkjent_ytelse (id)
    personIdent                         VARCHAR (20),
    stonad_fom                          TIMESTAMP(3),
    stonad_tom                          TIMESTAMP(3),
    belop                               bigint ,
    type                                VARCHAR (100)
);
