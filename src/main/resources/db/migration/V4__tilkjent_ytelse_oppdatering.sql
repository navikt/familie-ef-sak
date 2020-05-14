ALTER TABLE tilkjent_ytelse
    ALTER COLUMN stonad_fom TYPE DATE,
    ALTER COLUMN stonad_tom TYPE DATE,
    ALTER COLUMN opphor_fom TYPE DATE,
    ALTER COLUMN vedtaksdato TYPE DATE,
    ADD COLUMN   versjon            bigint        default 0                       not null,
    ADD COLUMN   opprettet_av       VARCHAR(512)  default 'VL'                    not null,
    ADD COLUMN   opprettet_tid      TIMESTAMP(3)  default localtimestamp          not null,
    ADD COLUMN   endret_av          VARCHAR(512),
    ADD COLUMN   endret_tid         TIMESTAMP(3);

ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN stonad_fom TYPE DATE,
    ALTER COLUMN stonad_tom TYPE DATE,
    ADD COLUMN   versjon            bigint        default 0                       not null,
    ADD COLUMN   opprettet_av       VARCHAR(512)  default 'VL'                    not null,
    ADD COLUMN   opprettet_tid      TIMESTAMP(3)  default localtimestamp          not null,
    ADD COLUMN   endret_av          VARCHAR(512),
    ADD COLUMN   endret_tid         TIMESTAMP(3);


