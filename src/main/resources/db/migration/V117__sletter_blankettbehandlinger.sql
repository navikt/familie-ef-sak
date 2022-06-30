DELETE
FROM fagsak
WHERE id IN (SELECT fagsak_id
             FROM behandling
             WHERE behandling.type = 'BLANKETT');
