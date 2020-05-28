DROP TABLE andel_tilkjent_ytelse;
DROP TABLE tilkjent_ytelse;

CREATE SEQUENCE ytelse_lopenummer_seq INCREMENT BY 1000 START WITH 1000;


CREATE TABLE tilkjent_ytelse (
    id                       UUID PRIMARY KEY,
    periode_id_start         BIGINT           NOT NULL DEFAULT nextval('ytelse_lopenummer_seq'),
    type                     VARCHAR(50)      NOT NULL,
    status                   VARCHAR(50)      NOT NULL,
    personIdent              VARCHAR(20)      NOT NULL,
    saksnummer               VARCHAR(50)      NOT NULL,
    stonad_fom               DATE,
    stonad_tom               DATE,
    opphor_fom               DATE,
    utbetalingsoppdrag       VARCHAR,
    forrige_periode_id_start BIGINT,
    vedtaksdato              DATE,
    opprettet_av             VARCHAR          NOT NULL DEFAULT 'VL',
    opprettet_tid            TIMESTAMP(3) NOT NULL     DEFAULT localtimestamp,
    endret_av                VARCHAR,
    endret_tid               TIMESTAMP(3)              DEFAULT localtimestamp
);

CREATE TABLE andel_tilkjent_ytelse (
    tilkjent_ytelse     UUID REFERENCES tilkjent_ytelse (id),
    tilkjent_ytelse_key BIGINT       NOT NULL,
    stonad_fom          DATE         NOT NULL,
    stonad_tom          DATE         NOT NULL,
    belop               BIGINT       NOT NULL,
    type                VARCHAR(100) NOT NULL
);
