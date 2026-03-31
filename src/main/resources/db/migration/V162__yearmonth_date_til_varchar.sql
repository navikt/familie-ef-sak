ALTER TABLE uttrekk_arbeidssoker
    ALTER COLUMN aar_maaned TYPE VARCHAR USING TO_CHAR(aar_maaned, 'YYYY-MM');

ALTER TABLE tilkjent_ytelse
    ALTER COLUMN grunnbelopsdato TYPE VARCHAR USING TO_CHAR(grunnbelopsdato, 'YYYY-MM');

ALTER TABLE vedtak
    ALTER COLUMN opphor_fom TYPE VARCHAR USING TO_CHAR(opphor_fom, 'YYYY-MM');

ALTER TABLE soknadsskjema
    ALTER COLUMN soker_fra TYPE VARCHAR USING TO_CHAR(soker_fra, 'YYYY-MM');

ALTER TABLE soknad_tidligere_utdanning
    ALTER COLUMN fra TYPE VARCHAR USING TO_CHAR(fra, 'YYYY-MM');

ALTER TABLE soknad_tidligere_utdanning
    ALTER COLUMN til TYPE VARCHAR USING TO_CHAR(til, 'YYYY-MM');
