CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; --TODO slett? blir sikkert lagt inn i Charlie sin branch før denne

CREATE TABLE person (
    id        UUID PRIMARY KEY,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    fagsak_id UUID REFERENCES fagsak (id) -- fjernes senere i scriptet
);

CREATE TABLE person_ident (
    ident         VARCHAR PRIMARY KEY,
    person_id     UUID         NOT NULL REFERENCES person (id),
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ON person_ident (person_id);

-- Oppretter en person for hver fagsak
INSERT INTO person (id, fagsak_id) (SELECT uuid_generate_v4(), id FROM fagsak);

-- Kopierer tabellen fagsak_person til person_ident
INSERT INTO person_ident (ident, person_id, opprettet_av, opprettet_tid, endret_av, endret_tid)
(SELECT ident, p.id, fp.opprettet_av, fp.opprettet_tid, fp.endret_av, fp.endret_tid
FROM fagsak_person fp
JOIN person p ON fp.fagsak_id = p.fagsak_id);

-- Legger til person_id til fagsak og legger til index og ux
ALTER TABLE fagsak ADD COLUMN person_id UUID REFERENCES person (id);
CREATE INDEX ON fagsak (person_id);
ALTER TABLE fagsak ADD CONSTRAINT fagsak_person_unique UNIQUE (person_id, stonadstype);

UPDATE fagsak f SET person_id = p.id FROM person p WHERE p.fagsak_id = f.id;
ALTER TABLE fagsak ALTER COLUMN person_id SET NOT NULL;

--Fjerner støttekolonne i person
ALTER TABLE person DROP COLUMN fagsak_id;