CREATE TABLE uttrekk_arbeidssøkere (
    id         UUID PRIMARY KEY,
    fagsak_id  UUID NOT NULL,
    vedtak_id  UUID NOT NULL,
    maaned_aar DATE NOT NULL,
    endret_av  VARCHAR,
    endret_tid TIMESTAMP(3),
    sjekket    BOOLEAN
);

create