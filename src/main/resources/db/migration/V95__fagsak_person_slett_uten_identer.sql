-- Fjerner tomme fagsak_person som blitt lagt til pga manglende transaksjonshåndtering
-- Disse har ikke noe koblet til seg, hverken fagsak eller person_ident
DELETE
FROM fagsak_person fp
WHERE id IN (
    SELECT id
    FROM fagsak_person fp
             LEFT JOIN person_ident pi ON pi.fagsak_person_id = fp.id
    WHERE pi.ident IS NULL
);