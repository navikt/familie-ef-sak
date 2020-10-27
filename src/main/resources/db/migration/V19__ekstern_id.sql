CREATE SEQUENCE BEHANDLING_EKSTERN_ID_SEQ START WITH 1000000 NO CYCLE;
ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS ekstern_id bigint not null unique default nextval('BEHANDLING_EKSTERN_ID_SEQ');

CREATE SEQUENCE FAGSAK_EKSTERN_ID_SEQ START WITH 1000000 NO CYCLE;
ALTER TABLE fagsak
    ADD COLUMN IF NOT EXISTS ekstern_id bigint not null unique default nextval('FAGSAK_EKSTERN_ID_SEQ');

CREATE OR REPLACE FUNCTION behandling_sett_ekstern_id() returns trigger as
    $body$
    BEGIN
        update behandling
        set ekstern_id = nextval('BEHANDLING_EKSTERN_ID_SEQ')
        where id = new.id;
        return new;
    END
    $body$ LANGUAGE plpgsql;

CREATE TRIGGER behandling_ekstern_id_inc
    after insert
    on behandling
    for each row
    execute procedure sett_ekstern_id();

CREATE OR REPLACE FUNCTION fagsak_sett_ekstern_id() returns trigger as
    $body$
    BEGIN
        update fagsak
        set ekstern_id = nextval('FAGSAK_EKSTERN_ID_SEQ')
        where id = new.id;
        return new;
    END
    $body$ LANGUAGE plpgsql;

CREATE TRIGGER fagsak_ekstern_id_inc
    after insert
    on fagsak
    for each row
execute procedure fagsak_sett_ekstern_id();