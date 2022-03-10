CREATE TABLE migrering (
    ident  VARCHAR PRIMARY KEY,
    status VARCHAR NOT NULL,
    arsak  VARCHAR
);

INSERT INTO migrering (ident, status)
    (SELECT ident, 'OK'
     FROM fagsak f
              JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
     WHERE f.migrert = TRUE)
;