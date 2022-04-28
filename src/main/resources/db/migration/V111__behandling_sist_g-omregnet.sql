ALTER TABLE tilkjent_ytelse
    ADD COLUMN grunnbelopsdato DATE NOT NULL DEFAULT '2021-05-01';
ALTER TABLE tilkjent_ytelse
    ADD COLUMN g_omregning_versjon TIMESTAMP(3) NOT NULL DEFAULT '2021-05-01'