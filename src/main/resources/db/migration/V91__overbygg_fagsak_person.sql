CREATE TABLE fagsak_person (
    id            UUID PRIMARY KEY,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    fagsak_id     UUID REFERENCES fagsak (id)
);
-- fagsak_id fjernes senere i scriptet, brukes for å koble sammen fagsak_person_old til fagsak

CREATE TABLE person_ident (
    ident            VARCHAR PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ON person_ident (fagsak_person_id);

-- Oppretter en person for hver fagsak
INSERT INTO fagsak_person (id, fagsak_id) (SELECT uuid_generate_v4(), id FROM fagsak);

-- Kopierer tabellen fagsak_person til person_ident
INSERT INTO person_ident (ident, fagsak_person_id, opprettet_av, opprettet_tid, endret_av, endret_tid)
(SELECT ident, p.id, fp.opprettet_av, fp.opprettet_tid, fp.endret_av, fp.endret_tid
FROM fagsak_person_old fp
JOIN fagsak_person p ON fp.fagsak_id = p.fagsak_id);

-- Legger til person_id til fagsak og legger til index og ux
ALTER TABLE fagsak ADD COLUMN fagsak_person_id UUID REFERENCES fagsak_person (id);
CREATE INDEX ON fagsak (fagsak_person_id);
ALTER TABLE fagsak ADD CONSTRAINT fagsak_person_unique UNIQUE (fagsak_person_id, stonadstype);

UPDATE fagsak f SET fagsak_person_id = p.id FROM fagsak_person p WHERE p.fagsak_id = f.id;
ALTER TABLE fagsak ALTER COLUMN fagsak_person_id SET NOT NULL;

--Fjerner støttekolonne i person
ALTER TABLE fagsak_person DROP COLUMN fagsak_id;