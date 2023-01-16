CREATE UNIQUE INDEX idx_behandlinger_i_arbeid ON behandling (fagsak_id) WHERE (status <> 'FERDIGSTILT' AND status <> 'SATT_PÅ_VENT');
DROP INDEX behandlinger_i_arbeid;