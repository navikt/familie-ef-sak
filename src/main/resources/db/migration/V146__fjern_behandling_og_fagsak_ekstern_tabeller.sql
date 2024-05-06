---- Må flytte sequencene som brukes for å generere eksternId ut av tabellene fagsak_ekstern og behandling_ekstern før sletting - hvis ikke forsvinner de i dragsuget ---
ALTER SEQUENCE behandling_ekstern_id_seq OWNED BY NONE;
ALTER SEQUENCE fagsak_ekstern_id_seq OWNED BY NONE;

DROP TABLE fagsak_ekstern;
DROP TABLE behandling_ekstern;